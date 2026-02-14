package rpc.pazz.remote.transport.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
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

@Slf4j
public class RpcMessageDecoder extends LengthFieldBasedFrameDecoder {

    public RpcMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    public RpcMessageDecoder() {
        super(RpcConstants.MAX_FRAME_LENGTH, 5, 4, -9, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decode = super.decode(ctx, in);
        if (decode instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) decode;
            //只有大于协议头长度的才是有效网络包
            if (buf.readableBytes() >= RpcConstants.TOTAL_LENGTH) {
                try {
                    return decodeBuf(buf);
                } catch (Exception e) {
                    log.error("Decode frame error!");
                    throw e;
                } finally {
                    buf.release();
                }
            }
        }
        return decode;
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
            String codecName = SerializationTypeEnum.getName(codecType);
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
