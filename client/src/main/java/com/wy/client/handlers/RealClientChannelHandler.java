package com.wy.client.handlers;

import com.wy.ProxyMessage;
import com.wy.common.ChannelContainer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.StringUtil;

import static com.wy.ProxyMessage.TRANSMISSION;

/**
 * @Author: wy
 * @Date: Created in 17:32 2020/1/31
 * @Description: 真实客户端处理器
 * @Modified: By：
 */
public class RealClientChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    public RealClientChannelHandler() {
        super(false);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        System.out.println("收到真实服务端返回的数据");
//        if (msg.readableBytes() >= 5) {
//            if (msg.readByte() == ProxyMessage.TRANSMISSION && msg.readableBytes() == msg.readInt()) {
//                ctx.fireChannelRead(msg);
//                return;
//            }
//        }
        //获取id
        String id = ChannelContainer.container.inverse().get(ctx.channel());
        if (StringUtil.isNullOrEmpty(id)) {
            return;
        }
        //channel为 客户端<->真实客户端
        ProxyMessage proxyMessage = new ProxyMessage();
        int length = msg.readableBytes();
        proxyMessage.setLength(length);
        proxyMessage.setType(TRANSMISSION);
        byte[] bytes = new byte[length];
        msg.readBytes(bytes);
        proxyMessage.setData(bytes);
        proxyMessage.setId(Long.parseLong(id));
        //直接转发回服务端
        Channel client2ServerChannel = ChannelContainer.container.get("client2ServerChannel");
        client2ServerChannel.writeAndFlush(proxyMessage);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String id = ChannelContainer.container.inverse().remove(ctx.channel());
        if (!StringUtil.isNullOrEmpty(id)) {
            System.out.println("与真实服务端断开连接: id=" + id);
        }
        super.channelInactive(ctx);
    }
}
