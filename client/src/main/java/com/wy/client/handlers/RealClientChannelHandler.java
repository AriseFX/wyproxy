package com.wy.client.handlers;

import com.wy.ProxyMessage;
import com.wy.common.ChannelContainer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

import static com.wy.ProxyMessage.SERVICE_EXCEPTION;
import static com.wy.ProxyMessage.TRANSMISSION;

/**
 * @Author: wy
 * @Date: Created in 17:32 2020/1/31
 * @Description: 真实客户端处理器
 * @Modified: By：
 */
@Slf4j
public class RealClientChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    public RealClientChannelHandler() {
        super(false);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        log.info("收到真实服务端返回的数据");
        //获取id
        String id = ChannelContainer.getId(ctx.channel());
        if (StringUtil.isNullOrEmpty(id)) {
            return;
        }
        //channel为 客户端<->真实客户端
        ProxyMessage proxyMessage = new ProxyMessage();
        int length = msg.readableBytes();
        proxyMessage.setLength(length);
        proxyMessage.setType(TRANSMISSION);
        proxyMessage.setData(msg.nioBuffer());
        proxyMessage.setId(Long.parseLong(id));
        //直接转发回服务端
        Channel client2ServerChannel = ChannelContainer.getProxyChannel();
        client2ServerChannel.writeAndFlush(proxyMessage);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sendDisConnectMsg(ctx);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("real exception caught:{}", cause.getMessage());
        sendDisConnectMsg(ctx);
    }

    private void sendDisConnectMsg(ChannelHandlerContext ctx) {
        String id = ChannelContainer.remove(ctx.channel());
        if (id != null) {
            log.info("与真实服务端断开连接: id= {}", id);
            Channel proxyChannel = ChannelContainer.getProxyChannel();
            if (proxyChannel != null) {
                ProxyMessage proxyMessage = new ProxyMessage();
                proxyMessage.setType(SERVICE_EXCEPTION);
                proxyMessage.setData(ByteBuffer.allocate(0));
                proxyMessage.setId(Long.parseLong(id));
                proxyMessage.setLength(0);
                proxyChannel.writeAndFlush(proxyMessage);
                log.warn("连接断开，请检查真实服务端状态");
            }
        }
    }
}
