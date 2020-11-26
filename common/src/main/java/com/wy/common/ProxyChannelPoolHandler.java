package com.wy.common;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.util.concurrent.Future;

/**
 * @Author: wy
 * @Date: Created in 10:00 2020/11/25
 * @Description:
 * @Modified: By：
 */
public interface ProxyChannelPoolHandler extends ChannelPoolHandler, ChannelHealthChecker {

    @Override
    default Future<Boolean> isHealthy(Channel channel) {
        //TODO 健康检查
        return channel.isActive() ? null : null;
    }
}
