package rpc.petrel.remote.transport.socket.codec;

import lombok.extern.slf4j.Slf4j;
import rpc.petrel.compress.Compress;
import rpc.petrel.enums.CompressTypeEnum;
import rpc.petrel.enums.SerializationTypeEnum;
import rpc.petrel.extension.ExtensionLoader;
import rpc.petrel.remote.constants.RpcConstants;
import rpc.petrel.remote.dto.RpcMessage;
import rpc.petrel.remote.dto.RpcRequest;
import rpc.petrel.remote.dto.RpcResponse;
import rpc.petrel.serialize.Serializer;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RpcMessageStreamCodec {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    public void writeAndFlushWithEncode(OutputStream outputStream, RpcMessage rpcMessage) throws IOException {
        DataOutputStream out = new DataOutputStream(outputStream);

        out.write(RpcConstants.MAGIC_NUMBER);
        out.writeByte(RpcConstants.VERSION);

        byte messageType = rpcMessage.getMessageType();
        byte codecType = rpcMessage.getCodec();
        byte compressType = rpcMessage.getCompress();
        int requestId = rpcMessage.getRequestId() == 0 ? ATOMIC_INTEGER.getAndIncrement() : rpcMessage.getRequestId();

        byte[] body = null;
        int fullLength = RpcConstants.HEAD_LENGTH;
        if (messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            String codecName = SerializationTypeEnum.getName(codecType);
            log.debug("use codec :{}", codecName);
            Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
            body = serializer.serialize(rpcMessage.getData());

            String compressName = CompressTypeEnum.getName(compressType);
            log.debug("use compress :{}", compressName);
            Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
            body = compress.compress(body);
            fullLength += body.length;
        }

        out.writeInt(fullLength);
        out.writeByte(messageType);
        out.writeByte(codecType);
        out.writeByte(compressType);
        out.writeInt(requestId);

        if (body != null) {
            out.write(body);
        }
        out.flush();
    }

    public RpcMessage decodeAndReadMessage(InputStream inputStream) throws IOException {
        DataInputStream in = new DataInputStream(inputStream);

        byte[] magicNumber = new byte[RpcConstants.MAGIC_NUMBER.length];
        in.readFully(magicNumber);
        checkMagicNumber(magicNumber);

        byte version = in.readByte();
        checkVersion(version);

        int fullLength = in.readInt();
        byte messageType = in.readByte();
        byte codecType = in.readByte();
        byte compressType = in.readByte();
        int requestId = in.readInt();

        RpcMessage rpcMessage = RpcMessage.builder()
                .messageType(messageType)
                .codec(codecType)
                .compress(compressType)
                .requestId(requestId)
                .build();

        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            log.debug("received heartbeat request");
            rpcMessage.setData(RpcConstants.PING);
            return rpcMessage;
        }
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            log.debug("received heartbeat response");
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }

        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
        if (bodyLength > 0) {
            byte[] body = new byte[bodyLength];
            in.readFully(body);

            String compressName = CompressTypeEnum.getName(compressType);
            log.debug("use compress: {}", compressName);
            Compress compress = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
            body = compress.decompress(body);

            String codecName = SerializationTypeEnum.getName(codecType);
            log.debug("use codec: {}", codecName);
            Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
            if (messageType == RpcConstants.REQUEST_TYPE) {
                RpcRequest request = serializer.deserialize(body, RpcRequest.class);
                rpcMessage.setData(request);
            } else {
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
