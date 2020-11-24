package com.wy.server.handlers;

import com.wy.ProxyMessage;
import com.wy.common.ChannelContainer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import static com.wy.ProxyMessage.*;
import static java.nio.charset.StandardCharsets.UTF_8;

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
                ChannelContainer.setProxyChannel(ctx.channel());
                break;
            case TRANSMISSION:
                //通过id取出channel,转发给客户端
                Channel channel = ChannelContainer.getChannel(msg.getId() + "");
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
            case SERVICE_EXCEPTION:
                log.info("连不上真实客户端!");
                channel = ChannelContainer.getChannel(msg.getId() + "");
                if (channel != null) {
                    ByteBuf buf = ctx.alloc().buffer(msg.getLength());
                    buf.writeBytes(_503bytes);
                    channel.writeAndFlush(buf);
                    channel.close();
                }
                break;
            default:
                break;
        }
    }
}
