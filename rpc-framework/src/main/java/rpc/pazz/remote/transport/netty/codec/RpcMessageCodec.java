package rpc.pazz.remote.transport.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;
import rpc.pazz.compress.Compress;
import rpc.pazz.enums.CompressTypeEnum;
import rpc.pazz.enums.SerializationTypeEnum;
import rpc.pazz.extension.ExtensionLoader;
import rpc.pazz.remote.constants.RpcConstants;
import rpc.pazz.remote.dto.RpcMessage;
import rpc.pazz.remote.dto.RpcRequest;
import rpc.pazz.remote.dto.RpcResponse;
import rpc.pazz.serialize.Serializer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RpcMessageCodec extends MessageToMessageCodec<ByteBuf, RpcMessage> {

    //as the global request id
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, List<Object> list) throws Exception {
        ByteBuf out = ctx.alloc().buffer();
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
            out.writerIndex(RpcConstants.MAGIC_NUMBER.length + 1);
            out.writeInt(fullLength);
            out.writerIndex(writerIndex);//重新置到末尾
            log.info("Bytebuf:{}", out);

            list.add(out);
        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> list) throws Exception {
        //只有大于协议头长度的才是有效网络包
        if (in.readableBytes() >= RpcConstants.TOTAL_LENGTH) {
            list.add(decodeBuf(in));
        }
    }

    private Object decodeBuf(ByteBuf in) {
        //检查魔数和协议版本
        byte[] magicNumber = new byte[RpcConstants.MAGIC_NUMBER.length];
        in.readBytes(magicNumber);
        checkMagicNumber(magicNumber);
        byte version = in.readByte();
        checkVersion(version);
        //解析协议头其他部分
        int fullLength = in.readInt();
        byte messageType = in.readByte();
        byte codecType = in.readByte();
        byte compressType = in.readByte();
        int requestId = in.readInt();
        log.info("magicNumber:{}, version:{},", Arrays.toString(magicNumber));
        RpcMessage rpcMessage = RpcMessage.builder().messageType(messageType).codec(codecType).compress(compressType).requestId(requestId).build();
        //检查是不是心跳类消息
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setData(RpcConstants.PING);
            return rpcMessage;
        }
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }
        //是有内容的消息
        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
        if (bodyLength > 0) {
            byte[] body = new byte[bodyLength];
            in.readBytes(body);
            //按照发消息的顺序反过来，先解压缩
            String compressName = CompressTypeEnum.getName(compressType);
            log.info("use compress: {}", compressName);
            Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
            body = compress.decompress(body);
            //再反序列化
            String codecName = CompressTypeEnum.getName(codecType);
            log.info("use codec: {}", codecName);
            Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
            //因为序列化的内容可能是请求，也有可能是响应
            if (messageType == RpcConstants.REQUEST_TYPE) {
                RpcRequest request = serializer.deserialize(body, RpcRequest.class);
                rpcMessage.setData(request);
            } else {//是响应
                RpcResponse response = serializer.deserialize(body, RpcResponse.class);
                rpcMessage.setData(response);
            }
        }
        return rpcMessage;
    }

    private void checkMagicNumber(byte[] magicNumber) {
        int len = RpcConstants.MAGIC_NUMBER.length;
        for (int i = 0; i < len; i++) {
            if (RpcConstants.MAGIC_NUMBER[i] != magicNumber[i]) {
                throw new IllegalArgumentException("Invalid magic number :" + Arrays.toString(magicNumber));
            }
        }
    }

    private void checkVersion(byte version) {
        if (version != RpcConstants.VERSION) {
            throw new IllegalArgumentException("Invalid version :" + version);
        }
    }
}
