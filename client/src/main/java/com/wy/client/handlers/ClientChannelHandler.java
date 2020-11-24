package com.wy.client.handlers;

import com.wy.ProxyMessage;
import com.wy.client.ClientStarter;
import com.wy.common.ChannelContainer;
import io.netty.bootstrap.Bootstrap;
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

    private final Bootstrap realBootStrap;

    private final static String localHost = System.getProperty("localHost", "0.0.0.0");
    private final static String localPort = System.getProperty("localPort", "8099");

    public ClientChannelHandler(Bootstrap realBootStrap) {
        this.realBootStrap = realBootStrap;
    }

    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) {
        //传输类型 直接转发给真实客户端
        if (msg.getType() == TRANSMISSION) {
            log.info("代理客户端收到的(传输类型)消息id为: {}", msg.getId());
            if (ChannelContainer.container.containsKey(msg.getId() + "")) {
                Channel client2RealClientChannel = ChannelContainer.getChannel(msg.getId() + "");
                ByteBuf buf = ctx.alloc().buffer(msg.getLength());
                buf.writeBytes(msg.getData());
                client2RealClientChannel.writeAndFlush(buf);
                log.info("发送数据给真实服务端!");
            } else {
                connectRealServer(ctx, msg);
                channelRead0(ctx, msg);
                log.info("重新连接到真实客户端");
            }
        } else if (msg.getType() == CONNECTION) {
            connectRealServer(ctx, msg);
        } else if (msg.getType() == DIS_CONNECTION) {
            Channel channel = ChannelContainer.container.remove(msg.getId() + "");
            if (channel != null) {
                channel.close();
            }
        }
    }

    private void connectRealServer(ChannelHandlerContext ctx, ProxyMessage msg) {
        //获取id
        long id = msg.getId();
        log.info("代理客户端收到的 连接 消息id为：{}", id);
        //连接真实服务端
        try {
            ChannelFuture sync = realBootStrap.connect(localHost, Integer.parseInt(localPort))
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            Channel channel = future.channel();
                            ChannelContainer.addMapping(id + "", channel);
                            log.info("连接真实服务端成功! ");
                        } else {
                            ProxyMessage proxyMessage = new ProxyMessage();
                            proxyMessage.setType(SERVICE_EXCEPTION);
                            proxyMessage.setData(ByteBuffer.allocate(0));
                            proxyMessage.setId(id);
                            proxyMessage.setLength(0);
                            ctx.channel().writeAndFlush(proxyMessage);
                            log.warn("连接失败，请检查真实服务端状态");
                        }
                    }).sync();
            sync.channel().closeFuture();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
