package rpc.petrel.remote.transport.socket.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import rpc.petrel.extension.ExtensionLoader;
import rpc.petrel.factory.SingletonFactory;
import rpc.petrel.properties.RpcProperties;
import rpc.petrel.registry.ServiceDiscovery;
import rpc.petrel.remote.constants.RpcConstants;
import rpc.petrel.remote.dto.RpcMessage;
import rpc.petrel.remote.dto.RpcRequest;
import rpc.petrel.remote.dto.RpcResponse;
import rpc.petrel.remote.transport.RpcRequestTransport;
import rpc.petrel.remote.transport.socket.codec.RpcMessageStreamCodec;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class SocketRpcClient implements RpcRequestTransport {

    private final SocketProvider socketProvider = SingletonFactory.getInstance(SocketProvider.class);
    private final RpcProperties properties = SingletonFactory.getInstance(RpcProperties.class);
    private final ServiceDiscovery serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension(properties.getRegistryType().getName());//使用Zookeeper作为注册中心
    private final RpcMessageStreamCodec codec = SingletonFactory.getInstance(RpcMessageStreamCodec.class);

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        //获取连接
        InetSocketAddress address = this.serviceDiscovery.lookupService(rpcRequest);
        log.info("Sending RPC request to {}", address);
        Socket socket = this.getSocket(address);

        //包装请求
        RpcMessage rpcMessage = RpcMessage.builder()
                .messageType(RpcConstants.REQUEST_TYPE)
                .codec(properties.getSerializationType().getCode())
                .compress(properties.getCompressionType().getCode())
                .data(rpcRequest).build();

        try {
            OutputStream out = socket.getOutputStream();
            codec.writeAndFlushWithEncode(out, rpcMessage);
            InputStream in = socket.getInputStream();
            RpcMessage responseMessage = codec.decodeAndReadMessage(in);
            byte messageType = responseMessage.getMessageType();
            if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                log.info("Heartbeat [{}]", rpcMessage.getData());
            } else if (messageType == RpcConstants.RESPONSE_TYPE) {
                log.info("Response [{}]", rpcMessage.getData());
            }
            return responseMessage.getData();
        } catch (IOException e) {
            log.error("Unable to send rpc request", e);
            throw new RuntimeException(e);
        }
    }

    private Socket getSocket(InetSocketAddress address) {
        Socket socket = socketProvider.get(address);
        if (socket == null) {
            try {
                socket = new Socket();
                socket.connect(address);
                socketProvider.set(address, socket);
            } catch (IOException e) {
                log.error("Socket connect failed:", e);
            }
        }
        return socket;
    }
}
