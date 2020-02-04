package com.wy.client.handlers;

import com.wy.ProxyMessage;
import com.wy.common.ChannelContainer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import static com.wy.ProxyMessage.*;

/**
 * @Author: wy
 * @Date: Created in 19:08 2020/1/30
 * @Description: 消息入栈
 * @Modified: By：
 */
@Slf4j
public class ClientChannelHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    private Bootstrap realBootStrap;

    public ClientChannelHandler(Bootstrap realBootStrap) {
        this.realBootStrap = realBootStrap;
    }

    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) throws InterruptedException {
        //传输类型 直接转发给真实客户端
        if (msg.getType() == TRANSMISSION) {
            log.info("代理客户端收到的(传输类型)消息id为: {}", msg.getId());
            if (ChannelContainer.container.containsKey(msg.getId() + "")) {
                Channel client2RealClientChannel = ChannelContainer.container.get(msg.getId() + "");
                ByteBuf buf = ctx.alloc().buffer(msg.getLength());
                buf.writeBytes(msg.getData());
                client2RealClientChannel.writeAndFlush(buf);
                log.info("发送数据给真实服务端!");
            } else {
                log.info("代理客户端中没有该 id");
            }
        } else if (msg.getType() == CONNECTION) {
            //获取id
            String id = msg.getId() + "";
            log.info("代理客户端收到的 连接 消息id为：{}", id);
            //连接真实服务端
            try {
                ChannelFuture sync = realBootStrap.connect("127.0.0.1", 8080).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) {
                        if (future.isSuccess()) {
                            Channel channel = future.channel();
                            ChannelContainer.container.put(id, channel);
                            log.info("连接真实服务端成功! ");
                        } else {
                            log.warn("连接失败，请检查真实服务端状态");
                        }
                    }

                }).sync();
                sync.channel().closeFuture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (msg.getType() == DIS_CONNECTION) {
            Channel channel = ChannelContainer.container.remove(msg.getId() + "");
            if (channel != null) {
                channel.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exception caught", cause);
        super.exceptionCaught(ctx, cause);
    }
}
