package com.wy;

import com.wy.common.ChannelContainer;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import static com.wy.ProxyMessage.*;

/**
 * @Author: wy
 * @Date: Created in 17:03 2020/2/4
 * @Description: 心跳检测
 * @Modified: By：
 */
public class WyProxyIdleStateHandler extends IdleStateHandler {

    private static final int readerIdleTimeSeconds = 0;
    private static final int writerIdleTimeSeconds = 0;
    private static final int allIdleTimeSeconds = 30;

    public WyProxyIdleStateHandler() {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
        if (evt == IdleStateEvent.ALL_IDLE_STATE_EVENT) {
            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setId(-1);
            proxyMessage.setLength(0);
            proxyMessage.setType(HEARTBEAT);
            //发送心跳消息
            ctx.channel().writeAndFlush(proxyMessage).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    //失败,移除并关闭相应channel
                    ChannelContainer.remove(ctx.channel());
                    ctx.channel().close();
                }
            });
        }
        ctx.fireUserEventTriggered(evt);
    }
}
