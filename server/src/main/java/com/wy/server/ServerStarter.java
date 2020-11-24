package com.wy.server;

import com.wy.ProxyMessageDecoder;
import com.wy.ProxyMessageEncoder;
import com.wy.server.handlers.ProxyServerChannelHandler;
import com.wy.server.handlers.ServerChannelHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @Author: wy
 * @Date: Created in 22:20 2020/1/29
 * @Description:
 * @Modified: By：
 */
public class ServerStarter {

    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("boss"));
        EventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("worker"));
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new ProxyServerChannelHandler());
                            pipeline.addLast(new ProxyMessageEncoder());
                            pipeline.addLast(new ProxyMessageDecoder());
                            pipeline.addLast(new ServerChannelHandler());
                        }
                    });

            //获取系统参数
            ChannelFuture channelFuture = serverBootstrap.bind(System.getProperty("host", "127.0.0.1"), Integer.parseInt(System.getProperty("port", "9888"))).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
