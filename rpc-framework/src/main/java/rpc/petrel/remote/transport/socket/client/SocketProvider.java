package rpc.petrel.remote.transport.socket.client;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 复用已有连接
 */
@Slf4j
public class SocketProvider {

    private final Map<String, Socket> socketMap = new ConcurrentHashMap<>();

    public Socket get(InetSocketAddress remoteAddress) {
        String key = remoteAddress.toString();
        if (socketMap.containsKey(key)) {
            Socket socket = socketMap.get(key);
            if (socket != null && !socket.isClosed() && socket.isConnected()) {
                return socket;
            } else {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    log.warn("无法关闭Socket:{}", remoteAddress.toString(), e);
                } finally {
                    socketMap.remove(key);
                }
            }
        }
        return null;
    }

    public void set(InetSocketAddress remoteAddress, Socket socket) {
        try {
            socket.setKeepAlive(true);
        } catch (SocketException e) {
            log.error("无法开启Socket:{} 的长连接功能", remoteAddress.toString(), e);
        }
        String key = remoteAddress.toString();
        socketMap.put(key, socket);
    }

    public void remove(InetSocketAddress remoteAddress) {
        String key = remoteAddress.toString();
        try {
            if (socketMap.containsKey(key)) {
                Socket socket = socketMap.get(key);
                socket.close();
            }
        } catch (IOException e) {
            log.warn("无法关闭Socket:{}", remoteAddress.toString(), e);
        } finally {
            socketMap.remove(key);
        }

    }
}
