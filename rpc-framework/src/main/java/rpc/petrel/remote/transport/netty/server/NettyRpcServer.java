package rpc.petrel.remote.transport.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;
import rpc.petrel.config.CustomShutdownHook;
import rpc.petrel.remote.transport.RpcServer;
import rpc.petrel.remote.transport.netty.codec.RpcMessageCodec;
import rpc.petrel.remote.transport.netty.codec.RpcMessageFrameDecoder;
import rpc.petrel.utils.ThreadPoolFactoryUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

@Slf4j
//@Component // 程序启动时需要从Bean找到对应Server并启动 //改用SPI获取实例
public class NettyRpcServer implements RpcServer {

    public static final int PORT = 9998;

    @Override
    public void start() {
        try {
            log.info("Netty RpcServer starting...");
            //对象准备
            CustomShutdownHook.getCustomShutdownHook().clearAll();//在程序关闭时清理连接资源
            String host = InetAddress.getLocalHost().getHostAddress();
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                    Runtime.getRuntime().availableProcessors() * 2 /*cpu核心数目 * 2*/,
                    ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false)
            );
            //创建bootstrap
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)//TCP
                        .childOption(ChannelOption.TCP_NODELAY, true)// 开启 Nagle 算法尽可能的发送大数据快，减少网络传输
                        .childOption(ChannelOption.SO_KEEPALIVE, true)// 开启 TCP 底层心跳机制
                        .option(ChannelOption.SO_BACKLOG, 128) //用于临时存放已完成三次握手的请求的队列的最大长度
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new ChannelInitializer<SocketChannel>() {

                            @Override
                            protected void initChannel(SocketChannel socketChannel) throws Exception {
                                ChannelPipeline p = socketChannel.pipeline();
                                p.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                                p.addLast(new RpcMessageFrameDecoder());
                                p.addLast(new RpcMessageCodec());
                                //p.addLast(serviceHandlerGroup, new NettyRpcServerHandler()); // 可共享的 serverHandler
                                p.addLast(new Netty5RpcServerHandler(serviceHandlerGroup));//Netty5 写法
                            }
                        });
                // 绑定端口，同步等待绑定成
                ChannelFuture f = bootstrap.bind(host,PORT).sync();
                // 等待服务端监听端口关闭
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                log.error("Occur exception when start server:", e);
            } finally {
                log.info("Shutdown bossGroup and workerGroup");
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                serviceHandlerGroup.shutdownGracefully();
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
