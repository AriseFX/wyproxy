package com.wy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;



/**
 * @Author: wy
 * @Date: Created in 22:26 2020/1/29
 * @Description: 代理消息帧解码器
 * @Modified: By：
 */
@Slf4j
public class ProxyMessageDecoder extends LengthFieldBasedFrameDecoder {

    private static final int maxFrameLength = 8192;
    private static final int lengthFieldOffset = 4;
    private static final int lengthFieldLength = 4;
    private static final int lengthAdjustment = 9;
    private static final int initialBytesToStrip = 4;

    public ProxyMessageDecoder() {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected ProxyMessage decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        in = (ByteBuf) super.decode(ctx, in);
        if (in == null) {
            return null;
        }
        //长度
        int length = in.readInt();
        //类型
        byte type = in.readByte();
        //id
        long id = in.readLong();
        //数据
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        ProxyMessage proxyMessage = new ProxyMessage();
        proxyMessage.setData(bytes);
        proxyMessage.setLength(length);
        proxyMessage.setType(type);
        proxyMessage.setId(id);
        return proxyMessage;
    }
}
