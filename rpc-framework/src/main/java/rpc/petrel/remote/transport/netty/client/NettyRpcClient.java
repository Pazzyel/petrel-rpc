package rpc.petrel.remote.transport.netty.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import rpc.petrel.enums.ServiceDiscoveryEnum;
import rpc.petrel.extension.ExtensionLoader;
import rpc.petrel.factory.SingletonFactory;
import rpc.petrel.properties.RpcProperties;
import rpc.petrel.registry.ServiceDiscovery;
import rpc.petrel.remote.constants.RpcConstants;
import rpc.petrel.remote.dto.RpcMessage;
import rpc.petrel.remote.dto.RpcRequest;
import rpc.petrel.remote.dto.RpcResponse;
import rpc.petrel.remote.transport.RpcRequestTransport;
import rpc.petrel.remote.transport.netty.codec.RpcMessageCodec;
import rpc.petrel.remote.transport.netty.codec.RpcMessageFrameDecoder;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class NettyRpcClient implements RpcRequestTransport {

    private final ServiceDiscovery serviceDiscovery;
    private final UnprocessedRequests unprocessedRequests;
    private final ChannelProvider channelProvider;

    private final EventLoopGroup eventLoopGroup;
    private final Bootstrap bootstrap;
    private final RpcProperties properties;

    public NettyRpcClient() {
        this.properties = SingletonFactory.getInstance(RpcProperties.class);
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension(properties.getRegistryType().getName());//使用Zookeeper作为注册中心
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);


        RpcMessageCodec codec = new RpcMessageCodec();//有状态的类不要用单例

        this.eventLoopGroup = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();

        this.bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class) // TCP连接
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline p = socketChannel.pipeline();
                        //流水线工艺顺序不能调换
                        p.addLast(new IdleStateHandler(0, 5, 5));//每5s触发WRITER_IDLE
                        p.addLast(new RpcMessageFrameDecoder());//帧解码
                        p.addLast(new RpcMessageCodec());//协议解码
                        p.addLast(new NettyRpcClientHandler());//回调方法
                    }
                });
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {

        //获取连接
        InetSocketAddress address = this.serviceDiscovery.lookupService(rpcRequest);
        Channel channel = this.getChannel(address);

        CompletableFuture<RpcResponse<Object>> future = new CompletableFuture<>();
        if (channel != null && channel.isActive()) {
            unprocessedRequests.put(rpcRequest.getRequestId(), future);
            RpcMessage rpcMessage = RpcMessage.builder()
                    .messageType(RpcConstants.REQUEST_TYPE)
                    .codec(properties.getSerializationType().getCode())
                    .compress(properties.getCompressionType().getCode())
                    .data(rpcRequest).build();
            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) f -> {
                //回调的主要作用是在失败时能够移除unprocessedRequests的对应项
                if (f.isSuccess()) {
                    log.info("Client send message: [{}]", rpcMessage);
                } else {
                    f.channel().close();
                    future.completeExceptionally(f.cause());
                    log.error("Send failed:", f.cause());
                }
            });
        } else {
            throw new IllegalStateException();
        }

        //获取结果
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException("rpc请求失败," + e.getMessage());
        }
    }

    //建立channel并缓存
    public Channel getChannel(InetSocketAddress remoteAddress) {
        Channel channel = channelProvider.get(remoteAddress);
        if (channel == null) {
            channel = doConnect(remoteAddress);
            channelProvider.set(remoteAddress, channel);
        }
        return channel;
    }

    //建立channel
    public Channel doConnect(InetSocketAddress address) {
        CompletableFuture<Channel> future = new CompletableFuture<>();
        this.bootstrap.connect(address).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                log.info("The client has connected [{}] successful!", address.toString());
                future.complete(f.channel());
            } else {
                throw new RuntimeException("The client has failed to connect [" + address.toString() + "]!");
            }
        });
        try {
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
