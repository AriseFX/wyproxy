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
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

import static com.wy.ProxyMessage.CONNECTION;

/**
 * @Author: wy
 * @Date: Created in 17:42 2020/1/30
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class ClientStarter {

    private final static String remoteHost = System.getProperty("remoteHost", "39.99.222.172");

    private final static String remotePort = System.getProperty("remotePort", "9888");
    /**
     * 连接超时时间
     */
    private final static int connectTimeoutMillis = 10 * 1000;

    private static Bootstrap realBootStrap;

    public static void main(String[] args) {
        startReal();
        startProxy();
    }

    public static void startReal() {
        //real server Bootstrap(连接真实服务端)
        EventLoopGroup real_eventLoopGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("real_client_worker"));
        realBootStrap = new Bootstrap();
        realBootStrap.group(real_eventLoopGroup).channel(NioSocketChannel.class)
                .option(NioChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new RealClientChannelHandler());
                    }
                });
    }

    public static void startProxy() {
        EventLoopGroup proxy_eventLoopGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("client_worker"));
        try {
            Bootstrap proxyBootstrap = new Bootstrap();
            proxyBootstrap.group(proxy_eventLoopGroup).channel(NioSocketChannel.class)
                    .option(NioChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
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
            ChannelFuture channelFuture = proxyBootstrap.connect(remoteHost, Integer.parseInt(remotePort))
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            log.info("连接代理服务端成功！");
                            //存入客户端<->服务端 channel
                            Channel channel = future.channel();
                            ChannelContainer.setProxyChannel(channel);

                            ProxyMessage proxyMessage = new ProxyMessage();
                            proxyMessage.setType(CONNECTION);
                            proxyMessage.setData(ByteBuffer.allocate(0));
                            proxyMessage.setId(-1);
                            proxyMessage.setLength(0);
                            future.channel().writeAndFlush(proxyMessage);
                        }
                    }).sync();
            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            proxy_eventLoopGroup.shutdownGracefully();
        }
    }

}
