package com.wy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import javax.print.attribute.standard.NumberUp;


/**
 * @Author: wy
 * @Date: Created in 22:26 2020/1/29
 * @Description: 代理消息解码器
 * @Modified: By：
 */
public class ProxyMessageDecoder extends LengthFieldBasedFrameDecoder {

    private static final int maxFrameLength = 1024;
    private static final int lengthFieldOffset = 0;
    private static final int lengthFieldLength = 4;
    private static final int lengthAdjustment = 9;
    private static final int initialBytesToStrip = 0;

    public ProxyMessageDecoder() {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected ProxyMessage decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        in = (ByteBuf) super.decode(ctx, in);
        //长度
        if (in.readableBytes() < 4) {
            return null;
        }
        int length = in.readInt();
        //类型
        if (in.readableBytes() < 1) {
            return null;
        }
        byte type = in.readByte();
        //id
        if (in.readableBytes() < 8) {
            return null;
        }
        long id = in.readLong();
        //数据
        if (length == in.readableBytes()) {
            byte[] bytes = new byte[length];
            in.readBytes(bytes);
            ProxyMessage proxyMessage = new ProxyMessage();
            proxyMessage.setData(bytes);
            proxyMessage.setLength(length);
            proxyMessage.setType(type);
            proxyMessage.setId(id);
            return proxyMessage;
        }
        return null;
    }
}
