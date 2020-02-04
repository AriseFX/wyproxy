package com.wy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @Author: wy
 * @Date: Created in 17:03 2020/2/4
 * @Description: 心跳检测
 * @Modified: By：
 */
public class WyProxyIdleStateHandler extends IdleStateHandler {

    public WyProxyIdleStateHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {

        ctx.fireUserEventTriggered(evt);
    }
}
