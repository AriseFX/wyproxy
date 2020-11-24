package com.wy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @Author: wy
 * @Date: Created in 22:26 2020/1/29
 * @Description: wyProxy协议
 * @Modified: By：
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyMessage {
    /**
     * 连接消息
     */
    public static final byte CONNECTION = 0x01;

    /**
     * 传输消息
     */
    public static final byte TRANSMISSION = 0x02;

    /**
     * 断开连接消息
     */
    public static final byte DIS_CONNECTION = 0x03;

    /**
     * 心跳消息
     */
    public static final byte HEARTBEAT = 0x04;

    /**
     * 真实服务端异常
     */
    public static final byte SERVICE_EXCEPTION = 0x05;

    /**
     * 协议魔数
     */
    public static final int MAGIC_NUMBER = 0xCAFEDEAD;

    private int length;

    private byte type;

    private long id;

    private byte[] data;

    public static byte[] _503bytes = "503".getBytes(UTF_8);

    public void encode(ByteBuf out) {
        //魔数
        out.writeInt(MAGIC_NUMBER);
        out.writeInt(length);
        out.writeByte(type);
        out.writeLong(id);
        out.writeBytes(data);
    }
}
