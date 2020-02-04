package com.wy.server.handlers;

import com.wy.ProxyMessage;
import com.wy.common.ChannelContainer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import static com.wy.ProxyMessage.*;

/**
 * @Author: wy
 * @Date: Created in 20:55 2020/1/30
 * @Description: wyProxy客户端->wyProxy服务端
 * @Modified: By：
 */
@Slf4j
public class ServerChannelHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) {
        switch (msg.getType()) {
            case CONNECTION:
                ChannelContainer.addMapping("proxy_channel", ctx.channel());
                break;
            case TRANSMISSION:
                //通过id取出channel,转发给浏览器
                Channel channel = ChannelContainer.container.get(msg.getId() + "");
                if (channel != null) {
                    ByteBuf buf = ctx.alloc().buffer(msg.getLength());
                    buf.writeBytes(msg.getData());
                    channel.writeAndFlush(buf);
                }
                break;
            case HEARTBEAT:
                //TODO
                log.info("收到心跳消息!");
                break;
            default:
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exception caught", cause);
        super.exceptionCaught(ctx, cause);
    }

}
