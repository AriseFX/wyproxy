package com.wy.client;

import com.wy.ProxyMessage;
import com.wy.ProxyMessageDecoder;
import com.wy.ProxyMessageEncoder;
import com.wy.client.handlers.ClientChannelHandler;
import com.wy.client.handlers.RealClientChannelHandler;
import com.wy.common.ChannelContainer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.charset.StandardCharsets;

import static com.wy.ProxyMessage.CONNECTION;

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
            final Bootstrap bootstrap = new Bootstrap();
            //real server(连接真实服务端)
            Bootstrap realBootStrap = new Bootstrap();
            realBootStrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new RealClientChannelHandler());
                        }
                    });


            bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new ProxyMessageDecoder());
                            pipeline.addLast(new ProxyMessageEncoder());
                            pipeline.addLast(new ClientChannelHandler(realBootStrap));
                        }
                    });
            //129.211.25.62 127.0.0.1
            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 9800)
                    .addListener((ChannelFutureListener) future -> {

                        //存入客户端<->服务端 channel
                        Channel channel = future.channel();
                        ChannelContainer.container.put("client2ServerChannel", channel);

                        ProxyMessage proxyMessage = new ProxyMessage();
                        proxyMessage.setType(CONNECTION);
                        proxyMessage.setData("hello".getBytes(StandardCharsets.UTF_8));
                        proxyMessage.setId(-1);
                        proxyMessage.setLength("hello".getBytes().length);
                        future.channel().writeAndFlush(proxyMessage);

                        //开子线程连接真实客户端
                        /*new Thread(() -> {
                    EventLoopGroup eventLoopGroup1 = new NioEventLoopGroup();
                    Bootstrap bootstrap1 = new Bootstrap();
                    bootstrap1.group(eventLoopGroup1).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    ChannelContainer.container.put("client2RealClientChannel", ctx.channel());
                                    super.channelActive(ctx);
                                }
                            });
                        }
                    });

                    try {
                        ChannelFuture sync = bootstrap1.connect("127.0.0.1", 8080).sync();
                        sync.channel().closeFuture().sync();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("end------------------------>");
                }).start();*/

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
