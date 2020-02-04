package com.wy.server;

import com.wy.ProxyMessageDecoder;
import com.wy.ProxyMessageEncoder;
import com.wy.server.handlers.ProxyServerChannelHandler;
import com.wy.server.handlers.ServerChannelHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @Author: wy
 * @Date: Created in 22:20 2020/1/29
 * @Description:
 * @Modified: By：
 */
public class ServerStarter {

    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
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
            ChannelFuture channelFuture = serverBootstrap.bind("0.0.0.0", 9800).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
