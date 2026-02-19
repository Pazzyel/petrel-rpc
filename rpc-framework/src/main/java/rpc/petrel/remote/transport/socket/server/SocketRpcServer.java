package rpc.petrel.remote.transport.socket.server;

import lombok.extern.slf4j.Slf4j;
import rpc.petrel.config.CustomShutdownHook;
import rpc.petrel.remote.transport.RpcServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class SocketRpcServer implements RpcServer {

    //和Netty的一致，因为服务注册时注册的端口号是Netty的9998
    public static final int PORT = 9998;

    private ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void start() {
        try (ServerSocket server = new ServerSocket()) {
            String host = InetAddress.getLocalHost().getHostAddress();
            server.bind(new InetSocketAddress(host, PORT));
            log.info("Server started on adddress {}:{}", host, PORT);
            CustomShutdownHook.getCustomShutdownHook().clearAll();
            Socket socket;
            while ((socket = server.accept()) != null) {
                log.info("client connected [{}]", socket.getInetAddress());
                executor.execute(new SocketRpcServerHandler(socket));
            }
            executor.shutdown();
        } catch (IOException e) {
            log.error("occur IOException:", e);
        }
    }
}
