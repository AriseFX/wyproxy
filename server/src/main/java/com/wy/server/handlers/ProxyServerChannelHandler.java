package com.wy.server.handlers;

import com.google.common.collect.BiMap;
import com.wy.ProxyMessage;
import com.wy.common.ChannelContainer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @Author: wy
 * @Date: Created in 18:41 2020/1/30
 * @Description:
 * @Modified: By：
 */
@Slf4j
public class ProxyServerChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static AtomicLong idProducer = new AtomicLong(0);

    public ProxyServerChannelHandler() {
        super(false);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        Channel proxy_channel = ChannelContainer.container.get("proxy_channel");
        if (proxy_channel != null && proxy_channel != ctx.channel()) {
            try {
                BiMap<Channel, String> inverseContainer = ChannelContainer.container.inverse();
                String id = inverseContainer.get(ctx.channel());
                byte[] bytes = new byte[msg.readableBytes()];
                msg.readBytes(bytes);
                ProxyMessage proxyMessage = new ProxyMessage();
                proxyMessage.setType(ProxyMessage.TRANSMISSION);
                proxyMessage.setLength(bytes.length);
                proxyMessage.setData(bytes);
                proxyMessage.setId(Long.parseLong(id));
                log.info("代理服务端收到,id= {} Channel的消息,并转发给代理客户端!",id);
                proxy_channel.writeAndFlush(proxyMessage);
            } finally {
                //引用计数-1
                msg.release();
            }
            return;
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel userClientChannel = ctx.channel();
        log.info("代理服务端通道激活：{}", userClientChannel);
        //说明是浏览器之类的用户端建立的channel
        Channel proxy_channel = ChannelContainer.container.get("proxy_channel");
        if (proxy_channel != null && proxy_channel != userClientChannel) {
            //发消息使 客户端和真实客户端的建立连接
            //保存到浏览器的channel，识别
            long id = idProducer.incrementAndGet();
            //用户的client channel
            ChannelContainer.container.put(id + "", userClientChannel);
            //用户端的地址信息
            InetSocketAddress sa = (InetSocketAddress) userClientChannel.localAddress();
            byte[] bytes = (sa.getPort() + "").getBytes(StandardCharsets.UTF_8);

            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setType(ProxyMessage.CONNECTION);
            proxyMessage.setLength(bytes.length);
            proxyMessage.setData(bytes);
            proxyMessage.setId(id);
            log.info("与真实客户端连接成功，生成id为:{}", id);
            proxy_channel.writeAndFlush(proxyMessage);
            log.info("发送连接消息给代理客户端，id为:{}", id);
            return;
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel userClientChannel = ctx.channel();
        Channel proxy_channel = ChannelContainer.container.get("proxy_channel");
        if (proxy_channel != null && userClientChannel != proxy_channel) {
            log.info("通道关闭:{} ", ctx.channel());
            //移除该channel与id映射关系
            String id = ChannelContainer.container.inverse().remove(userClientChannel);
            if (StringUtil.isNullOrEmpty(id)) {
                return;
            }
            log.info("代理服务端移除channel成功");
            byte[] bytes = "DIS_CONNECTION".getBytes(StandardCharsets.UTF_8);

            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setType(ProxyMessage.DIS_CONNECTION);
            proxyMessage.setLength(bytes.length);
            proxyMessage.setData(bytes);
            proxyMessage.setId(Long.parseLong(id));
            proxy_channel.writeAndFlush(proxyMessage);
            log.info("发送断开连接消息给代理客户端! id为：{}", id);
            return;
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exception caught", cause);
        super.exceptionCaught(ctx, cause);
    }

}
