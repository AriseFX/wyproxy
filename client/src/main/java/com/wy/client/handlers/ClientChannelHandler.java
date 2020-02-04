package com.wy.client.handlers;

import com.wy.ProxyMessage;
import com.wy.common.ChannelContainer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.wy.ProxyMessage.*;

/**
 * @Author: wy
 * @Date: Created in 19:08 2020/1/30
 * @Description: 消息入栈
 * @Modified: By：
 */
@Slf4j
public class ClientChannelHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Bootstrap realBootStrap;

    public ClientChannelHandler(Bootstrap realBootStrap) {
        this.realBootStrap = realBootStrap;
    }

    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage msg) throws InterruptedException {
        //传输类型 直接转发给真实客户端
        if (msg.getType() == TRANSMISSION) {
            System.out.println("代理客户端收到的 传输 消息id为:" + msg.getId());
            if (ChannelContainer.container.containsKey(msg.getId() + "")) {
                Channel client2RealClientChannel = ChannelContainer.container.get(msg.getId() + "");
                ByteBuf buf = ctx.alloc().buffer(msg.getLength());
                buf.writeBytes(msg.getData());
                System.out.println("发送数据给真实客户端, {}" + client2RealClientChannel);
                client2RealClientChannel.writeAndFlush(buf);
                //保存channel信息
            } else {
                System.out.println("客户端收到的消息中没有该 id");
            }
        } else if (msg.getType() == CONNECTION) {
            //获取id
            String id = msg.getId() + "";
            System.out.println("代理客户端收到的 连接 消息id为：" + id);
            //连接真实服务端
            try {
                ChannelFuture sync = realBootStrap.connect("127.0.0.1", 8080).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) {
                        Channel channel = future.channel();
                        ChannelContainer.container.put(id, channel);
                        System.out.println("连接真实服务端成功! container:" + ChannelContainer.container);
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
}
