package com.wy.client;

import com.wy.ProxyMessage;
import com.wy.ProxyMessageDecoder;
import com.wy.ProxyMessageEncoder;
import com.wy.WyProxyIdleStateHandler;
import com.wy.client.handlers.ClientChannelHandler;
import com.wy.client.handlers.RealClientChannelHandler;
import com.wy.common.ChannelContainer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.charset.StandardCharsets;

import static com.wy.ProxyMessage.*;

/**
 * @Author: wy
 * @Date: Created in 17:42 2020/1/30
 * @Description:
 * @Modified: By：
 */
public class ClientStarter {

    public static void main(String[] args) {
        EventLoopGroup eventLoopGroup = null;
        try {
            eventLoopGroup = new NioEventLoopGroup();
            //连接proxy server(连接代理服务端服务端)
            final Bootstrap proxyBootstrap = new Bootstrap();
            //连接real server(连接真实服务端)
            final Bootstrap realBootStrap = new Bootstrap();
            realBootStrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new RealClientChannelHandler());
                        }
                    });

            proxyBootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new ProxyMessageDecoder());
                            pipeline.addLast(new ProxyMessageEncoder());
                            pipeline.addLast(new WyProxyIdleStateHandler());
                            pipeline.addLast(new ClientChannelHandler(realBootStrap));
                        }
                    });
            //129.211.25.62  127.0.0.1
            ChannelFuture channelFuture = proxyBootstrap.connect("127.0.0.1", 9800)
                    .addListener((ChannelFutureListener) future -> {

                        //存入客户端<->服务端 channel
                        Channel channel = future.channel();
                        ChannelContainer.addMapping("client2ServerChannel", channel);

                        ProxyMessage proxyMessage = new ProxyMessage();
                        proxyMessage.setType(CONNECTION);
                        proxyMessage.setData("hello".getBytes(StandardCharsets.UTF_8));
                        proxyMessage.setId(-1);
                        proxyMessage.setLength("hello".getBytes().length);
                        future.channel().writeAndFlush(proxyMessage);

                    }).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (eventLoopGroup != null) {
                eventLoopGroup.shutdownGracefully();
            }
        }


    }
}
