package rpc.petrel.remote.transport.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import rpc.petrel.compress.Compress;
import rpc.petrel.enums.CompressTypeEnum;
import rpc.petrel.enums.SerializationTypeEnum;
import rpc.petrel.extension.ExtensionLoader;
import rpc.petrel.remote.constants.RpcConstants;
import rpc.petrel.remote.dto.RpcMessage;
import rpc.petrel.serialize.Serializer;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RpcMessageEncoder extends MessageToByteEncoder<RpcMessage> {

    //as the global request id
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, ByteBuf out) throws Exception {
        try {
            //构建消息头
            out.writeBytes(RpcConstants.MAGIC_NUMBER);
            out.writeByte(RpcConstants.VERSION);
            //移动writeIndex，预留fullLength的空间
            out.writerIndex(out.writerIndex() + 4);
            byte messageType = rpcMessage.getMessageType();
            out.writeByte(messageType);
            out.writeByte(rpcMessage.getCodec());
            out.writeByte(rpcMessage.getCompress());
            out.writeInt(ATOMIC_INTEGER.getAndIncrement());

            //构建消息体
            byte[] body = null;
            int fullLength = RpcConstants.HEAD_LENGTH;//现在只有开头的长度
            //不是心跳类消息才有消息体
            if (messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                //根据请求内容获取序列化器并序列化
                String codecName = SerializationTypeEnum.getName(rpcMessage.getCodec());
                log.info("use codec :{}", codecName);
                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
                body = serializer.serialize(rpcMessage.getData());
                //根据请求内容获取压缩器并压缩
                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
                log.info("use compress :{}", compressName);
                Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
                body = compress.compress(body);
                //修改fullLength
                fullLength += body.length;
            }
            if (body != null) {
                out.writeBytes(body);
            }

            //重新写入开头的fullLength
            int writerIndex = out.writerIndex();
            out.writerIndex(RpcConstants.MAGIC_NUMBER.length + 1);
            out.writeInt(fullLength);
            out.writerIndex(writerIndex);//重新置到末尾
        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
    }
}
