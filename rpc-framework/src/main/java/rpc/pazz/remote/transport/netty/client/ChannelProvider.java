package rpc.pazz.remote.transport.netty.client;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelProvider {

    private final Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    public Channel get(InetSocketAddress remoteAddress) {
        String key = remoteAddress.toString();
        if (channelMap.containsKey(key)) {
            Channel channel = channelMap.get(key);
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                channelMap.remove(key);//Map存在但不活跃，是无效的channel，移除缓存
            }
        }
        return null;
    }

    public void set(InetSocketAddress remoteAddress, Channel channel) {
        String key = remoteAddress.toString();
        channelMap.put(key, channel);
    }

    public void remove(InetSocketAddress remoteAddress) {
        String key = remoteAddress.toString();
        channelMap.remove(key);
    }

}
