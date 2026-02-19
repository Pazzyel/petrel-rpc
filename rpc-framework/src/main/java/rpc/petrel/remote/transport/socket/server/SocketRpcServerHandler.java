package rpc.petrel.remote.transport.socket.server;

import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import rpc.petrel.enums.RpcResponseCodeEnum;
import rpc.petrel.factory.SingletonFactory;
import rpc.petrel.handler.RpcRequestHandler;
import rpc.petrel.properties.RpcProperties;
import rpc.petrel.provider.ServiceProvider;
import rpc.petrel.remote.constants.RpcConstants;
import rpc.petrel.remote.dto.RpcMessage;
import rpc.petrel.remote.dto.RpcRequest;
import rpc.petrel.remote.dto.RpcResponse;
import rpc.petrel.remote.transport.socket.codec.RpcMessageStreamCodec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

@Slf4j
public class SocketRpcServerHandler implements Runnable {

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
            InputStream in = socket.getInputStream();
            RpcMessage requestMessage = codec.decodeAndReadMessage(in);
            log.info("server receive msg: [{}] ", requestMessage);
            byte messageType = requestMessage.getMessageType();
            RpcMessage rpcMessage = RpcMessage.builder()
                    .codec(properties.getSerializationType().getCode())
                    .compress(properties.getCompressionType().getCode()).build();//RpcMessage发送是不构造requestId
            if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                //是心跳请求
                rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                rpcMessage.setData(RpcConstants.PONG);
            } else {
                //是普通请求
                RpcRequest rpcRequest = (RpcRequest) requestMessage.getData();
                rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                Object result = rpcRequestHandler.handle(rpcRequest);
                log.info("server get result: [{}] ", result);
                if (!socket.isClosed() && socket.isConnected()) {
                    //使用request的id构造对应的response
                    RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                    rpcMessage.setData(rpcResponse);
                } else {
                    log.error("server write fail: [{}] ", rpcRequest);
                    RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                    rpcMessage.setData(rpcResponse);
                }
            }
            OutputStream out = socket.getOutputStream();
            codec.writeAndFlushWithEncode(out, rpcMessage);
        } catch (IOException e) {
            log.error("处理来自{}的RPC请求失败:",socket.getInetAddress(), e);
        }
    }
}
