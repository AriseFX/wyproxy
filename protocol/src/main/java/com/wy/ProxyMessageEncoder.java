package com.wy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @Author: wy
 * @Date: Created in 22:26 2020/1/29
 * @Description: 代理消息编码器
 * @Modified: By：
 */
public class ProxyMessageEncoder extends MessageToByteEncoder<ProxyMessage> {

    /**
     * 消息结构 {length;type;data}
     *
     * @param ctx
     * @param msg
     * @param out
     * @throws Exception
     */
    protected void encode(ChannelHandlerContext ctx, ProxyMessage msg, ByteBuf out) throws Exception {
        msg.encode(out);
    }
}
