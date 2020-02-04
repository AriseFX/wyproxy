package com.wy.server.handlers;

import com.wy.ProxyMessage;
import com.wy.common.ChannelContainer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @Author: wy
 * @Date: Created in 20:55 2020/1/30
 * @Description: wyProxy客户端->wyProxy服务端
 * @Modified: By：
 */
public class ServerChannelHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) {
        if (msg != null) {
            if (msg.getType() == ProxyMessage.CONNECTION) {
                ChannelContainer.container.put("proxy_channel", ctx.channel());
            } else if (msg.getType() == ProxyMessage.TRANSMISSION) {
                //通过id取出channel,转发给浏览器
                Channel channel = ChannelContainer.container.get(msg.getId() + "");
                if (channel != null) {
                    ByteBuf buf = ctx.alloc().buffer(msg.getLength());
                    buf.writeBytes(msg.getData());
                    channel.writeAndFlush(buf);
                }
            }
        }
    }
}
