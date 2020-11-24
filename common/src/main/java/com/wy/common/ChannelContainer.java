package com.wy.common;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.util.internal.StringUtil;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author: wy
 * @Date: Created in 19:04 2020/1/30
 * @Description: Channel管理容器
 * @Modified: By：
 */
public class ChannelContainer {

    public static BiMap<String, Channel> container = Maps.synchronizedBiMap(HashBiMap.create(32));

    public static AtomicReference<Channel> proxyChannel_ref = new AtomicReference<>();

    public static Channel getChannel(String id) {
        return container.get(id);
    }

    public static String getId(Channel channel) {
        return container.inverse().get(channel);
    }

    public static Channel remove(String id) {
        return container.remove(id);
    }

    public static String remove(Channel id) {
        return container.inverse().remove(id);
    }

    public static void addMapping(String id, Channel channel) {
        if (StringUtil.isNullOrEmpty(id) || channel == null) {
            throw new NullPointerException();
        }
        container.forcePut(id, channel);
    }

    public static int getSize() {
        return container.size();
    }

    public static void removeAndCloseAll() {
        container.values().forEach(ChannelOutboundInvoker::close);
        container.clear();
    }

    public static Channel getProxyChannel() {
        return proxyChannel_ref.get();
    }

    public static void setProxyChannel(Channel channel) {
        proxyChannel_ref.getAndSet(channel);
    }
}
