package com.wy.client.handlers;

import com.wy.ProxyMessage;
import com.wy.client.ClientStarter;
import com.wy.common.ChannelContainer;
import com.wy.common.ProxyChannelPool;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;


import java.nio.ByteBuffer;

import static com.wy.ProxyMessage.*;

/**
 * @Author: wy
 * @Date: Created in 19:08 2020/1/30
 * @Description: 消息入栈
 * @Modified: By：
 */
@Slf4j
public class ClientChannelHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    private final ProxyChannelPool proxyChannelPool;

    public ClientChannelHandler(ProxyChannelPool proxyChannelPool) {
        this.proxyChannelPool = proxyChannelPool;
    }

    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) {
        //传输类型 直接转发给真实客户端
        String id = msg.getId() + "";
        switch (msg.getType()) {
            case TRANSMISSION: {
                log.info("代理客户端收到的(传输类型)消息id为: {}", id);
                Channel channel = ChannelContainer.getChannel(id);
                if (channel == null) {
                    proxyChannelPool.getNewChannel().addListener((ChannelFutureListener) e -> {
                        if (e.isSuccess()) {
                            Channel new_channel = e.channel();
                            ChannelContainer.addMapping(id, new_channel);
                            send2RealServer(ctx, msg, new_channel);
                        } else {
                            ProxyMessage proxyMessage = new ProxyMessage();
                            proxyMessage.setType(SERVICE_EXCEPTION);
                            proxyMessage.setData(ByteBuffer.allocate(0));
                            proxyMessage.setId(msg.getId());
                            proxyMessage.setLength(0);
                            ctx.channel().writeAndFlush(proxyMessage);
                            log.warn("连接失败，请检查真实服务端状态");
                        }
                    }).syncUninterruptibly();
                } else {
                    send2RealServer(ctx, msg, channel);
                }
                break;
            }
            case CONNECTION: {
                proxyChannelPool.getNewChannel().addListener((ChannelFutureListener) e -> {
                    if (e.isSuccess()) {
                        Channel new_channel = e.channel();
                        ChannelContainer.addMapping(id, new_channel);
                    }
                }).syncUninterruptibly();
                break;
            }
            case DIS_CONNECTION: {
                log.info("客户端收到断开连接消息: {}", id);
                Channel remove = ChannelContainer.remove(id);
                remove.close();
                break;
            }
        }
    }

    private void send2RealServer(ChannelHandlerContext ctx, ProxyMessage msg, Channel channel) {
        ByteBuf buf = ctx.alloc().buffer(msg.getLength());
        buf.writeBytes(msg.getData());
        channel.writeAndFlush(buf);
        log.info("发送数据给真实服务端!");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //断线重连
        int time = 0;
        for (; ; ) {
            try {
                if (time++ > 20) {
                    break;
                }
                log.info("重连服务端第{}次", time);
                ClientStarter.startProxy();
                break;
            } catch (Exception e) {
                int t = Math.min(time, 4);
                log.error("连接失败,异常:{},{}秒后重试", e.getMessage(), t * 5);
                synchronized (this) {
                    wait(5000 * t);
                }
            }
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exception caught:{}", cause.getMessage());
    }
}
