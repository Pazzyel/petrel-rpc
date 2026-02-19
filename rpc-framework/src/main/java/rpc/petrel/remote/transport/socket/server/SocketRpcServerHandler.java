package rpc.petrel.remote.transport.socket.server;

import lombok.extern.slf4j.Slf4j;
import rpc.petrel.enums.RpcResponseCodeEnum;
import rpc.petrel.factory.SingletonFactory;
import rpc.petrel.handler.RpcRequestHandler;
import rpc.petrel.properties.RpcProperties;
import rpc.petrel.remote.constants.RpcConstants;
import rpc.petrel.remote.dto.RpcMessage;
import rpc.petrel.remote.dto.RpcRequest;
import rpc.petrel.remote.dto.RpcResponse;
import rpc.petrel.remote.transport.socket.codec.RpcMessageStreamCodec;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

@Slf4j
public class SocketRpcServerHandler implements Runnable {

    private static final int SERVER_IDLE_TIMEOUT_MILLIS = 30_000;

    private final Socket socket;
    private final RpcRequestHandler rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    private final RpcProperties properties = SingletonFactory.getInstance(RpcProperties.class);
    private final RpcMessageStreamCodec codec = SingletonFactory.getInstance(RpcMessageStreamCodec.class);

    public SocketRpcServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(SERVER_IDLE_TIMEOUT_MILLIS);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            while (!socket.isClosed() && socket.isConnected()) {
                RpcMessage requestMessage;
                try {
                    requestMessage = codec.decodeAndReadMessage(in);
                } catch (SocketTimeoutException e) {
                    log.info("close idle socket [{}] after {} ms", socket.getInetAddress(), SERVER_IDLE_TIMEOUT_MILLIS);
                    break;
                } catch (EOFException e) {
                    log.info("client disconnected [{}]", socket.getInetAddress());
                    break;
                }

                if (requestMessage == null) {
                    break;
                }

                RpcMessage responseMessage = buildResponse(requestMessage);
                codec.writeAndFlushWithEncode(out, responseMessage);
            }
        } catch (IOException e) {
            log.error("process rpc request from [{}] failed", socket.getInetAddress(), e);
        } finally {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                log.warn("close socket [{}] failed", socket.getInetAddress(), e);
            }
        }
    }

    private RpcMessage buildResponse(RpcMessage requestMessage) {
        byte messageType = requestMessage.getMessageType();
        RpcMessage rpcMessage = RpcMessage.builder()
                .messageType(RpcConstants.RESPONSE_TYPE)
                .codec(properties.getSerializationType().getCode())
                .compress(properties.getCompressionType().getCode())
                .requestId(requestMessage.getRequestId())
                .build();

        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }

        RpcRequest rpcRequest = (RpcRequest) requestMessage.getData();
        Object result = rpcRequestHandler.handle(rpcRequest);

        if (!socket.isClosed() && socket.isConnected()) {
            RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
            rpcMessage.setData(rpcResponse);
        } else {
            RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
            rpcMessage.setData(rpcResponse);
        }
        return rpcMessage;
    }
}
