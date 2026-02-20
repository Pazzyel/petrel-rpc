package rpc.petrel.remote.transport.socket.client;

import lombok.extern.slf4j.Slf4j;
import rpc.petrel.exception.UnsupportedInvokeException;
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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class SocketRpcClient implements RpcRequestTransport {

    private static final int HEARTBEAT_INTERVAL_SECONDS = 5;

    private final SocketProvider socketProvider = SingletonFactory.getInstance(SocketProvider.class);
    private final RpcProperties properties = SingletonFactory.getInstance(RpcProperties.class);
    private final ServiceDiscovery serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class)
            .getExtension(properties.getRegistryType().getName());
    private final RpcMessageStreamCodec codec = SingletonFactory.getInstance(RpcMessageStreamCodec.class);

    private final Map<String, ReentrantLock> socketLocks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    private final Map<String, Long> lastBusinessRequestTime = new ConcurrentHashMap<>();

    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(1);

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        InetSocketAddress address = this.serviceDiscovery.lookupService(rpcRequest);
        String key = address.toString();
        ReentrantLock lock = socketLocks.computeIfAbsent(key, k -> new ReentrantLock());

        lock.lock();
        try {
            Socket socket = this.getSocket(address);
            lastBusinessRequestTime.put(key, System.currentTimeMillis());

            RpcMessage rpcMessage = RpcMessage.builder()
                    .messageType(RpcConstants.REQUEST_TYPE)
                    .codec(properties.getSerializationType().getCode())
                    .compress(properties.getCompressionType().getCode())
                    .data(rpcRequest)
                    .build();

            try {
                OutputStream out = socket.getOutputStream();
                codec.writeAndFlushWithEncode(out, rpcMessage);

                InputStream in = socket.getInputStream();
                RpcMessage responseMessage = codec.decodeAndReadMessage(in);
                if (responseMessage.getMessageType() != RpcConstants.RESPONSE_TYPE) {
                    throw new IllegalStateException("unexpected message type: " + responseMessage.getMessageType());
                }
                return responseMessage.getData();
            } catch (Exception e) {
                cleanupConnection(address);
                log.error("Unable to send rpc request", e);
                throw new RuntimeException(e);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Future<RpcResponse<Object>> sendRpcRequestAsync(RpcRequest rpcRequest) {
        throw new UnsupportedInvokeException("Socket client does not support async calls");
    }

    private Socket getSocket(InetSocketAddress address) {
        Socket socket = socketProvider.get(address);
        if (socket == null) {
            try {
                socket = new Socket();
                socket.connect(address);
                socketProvider.set(address, socket);
                lastBusinessRequestTime.put(address.toString(), System.currentTimeMillis());
                startHeartbeatTask(address);
            } catch (IOException e) {
                log.error("Socket connect failed:", e);
                throw new RuntimeException(e);
            }
        }
        return socket;
    }

    //每一次RPC请求发送，就定下一个5s后触发的延时任务，间隔5s触发一次
    private void startHeartbeatTask(InetSocketAddress address) {
        String key = address.toString();
        heartbeatTasks.computeIfAbsent(key, k -> heartbeatExecutor.scheduleAtFixedRate(
                () -> sendHeartbeatIfIdle(address),
                HEARTBEAT_INTERVAL_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS,
                TimeUnit.SECONDS));
    }

    //延时任务触发，加锁发送一次heartbeat
    private void sendHeartbeatIfIdle(InetSocketAddress address) {
        String key = address.toString();
        Long lastRequestTime = lastBusinessRequestTime.get(key);
        if (lastRequestTime == null || System.currentTimeMillis() - lastRequestTime < HEARTBEAT_INTERVAL_SECONDS * 1000L) {
            return;
        }

        ReentrantLock lock = socketLocks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            Socket socket = socketProvider.get(address);
            if (socket == null) {
                cancelHeartbeatTask(key);
                return;
            }

            Long latest = lastBusinessRequestTime.get(key);
            if (latest == null || System.currentTimeMillis() - latest < HEARTBEAT_INTERVAL_SECONDS * 1000L) {
                return;
            }

            RpcMessage heartbeat = RpcMessage.builder()
                    .messageType(RpcConstants.HEARTBEAT_REQUEST_TYPE)
                    .codec(properties.getSerializationType().getCode())
                    .compress(properties.getCompressionType().getCode())
                    .data(RpcConstants.PING)
                    .build();

            try {
                OutputStream out = socket.getOutputStream();
                codec.writeAndFlushWithEncode(out, heartbeat);

                InputStream in = socket.getInputStream();
                RpcMessage response = codec.decodeAndReadMessage(in);
                if (response.getMessageType() != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                    throw new IllegalStateException("unexpected heartbeat response type: " + response.getMessageType());
                }
                log.debug("send heartbeat to [{}] and receive [{}]", address, response.getData());
            } catch (Exception e) {
                log.warn("heartbeat failed for [{}]", address, e);
                cleanupConnection(address);
            }
        } finally {
            lock.unlock();
        }
    }

    private void cleanupConnection(InetSocketAddress address) {
        String key = address.toString();
        cancelHeartbeatTask(key);
        lastBusinessRequestTime.remove(key);
        socketLocks.remove(key);
        socketProvider.remove(address);
    }

    private void cancelHeartbeatTask(String key) {
        ScheduledFuture<?> future = heartbeatTasks.remove(key);
        if (future != null) {
            future.cancel(true);
        }
    }
}
