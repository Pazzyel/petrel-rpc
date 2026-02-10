package rpc.pazz.remote.transport.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import rpc.pazz.compress.Compress;
import rpc.pazz.enums.CompressTypeEnum;
import rpc.pazz.enums.SerializationTypeEnum;
import rpc.pazz.extension.ExtensionLoader;
import rpc.pazz.remote.constants.RpcConstants;
import rpc.pazz.remote.dto.RpcMessage;
import rpc.pazz.serialize.Serializer;

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
            out.writeByte(CompressTypeEnum.GZIP.getCode());
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
                body = serializer.serialize(rpcMessage);
                //根据请求内容获取压缩器并压缩
                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
                log.info("use compress :{}", compressName);
                Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
                body = compress.compress(body);
                //修改fullLength
                fullLength += body.length;
            }

            //重新写入开头的fullLength
            int writerIndex = out.writerIndex();
            out.writerIndex(writerIndex - fullLength + RpcConstants.MAX_FRAME_LENGTH + 1);
            out.writeInt(fullLength);
            out.writerIndex(writerIndex);//重新置到末尾
        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
    }
}
