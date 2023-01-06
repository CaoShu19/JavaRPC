package com.cs.rpc.netty.client.cache;

import io.netty.channel.Channel;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：cs
 * @description：缓存channel
 * @date ：2022/11/23 6:30
 */
public class ChannelCache {
    private final Map<String, Channel> channelMap;

    public ChannelCache(){
        channelMap = new ConcurrentHashMap<>();
    }

    public Channel get(InetSocketAddress address){
        String key = address.toString();
        if(key != null){
            Channel channel = channelMap.get(key);
            if(channel != null && channel.isActive()){
                return channel;
            }else {
                remove(address);
            }
        }
        return null;
    }
    public void set(InetSocketAddress address,Channel channel){
        channelMap.put(address.toString(),channel);
    }
    public void remove(InetSocketAddress address){
        channelMap.remove(address.toString());
    }



}
