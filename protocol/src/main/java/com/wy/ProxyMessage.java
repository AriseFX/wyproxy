package com.wy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: wy
 * @Date: Created in 22:26 2020/1/29
 * @Description: 代理消息
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

    private int length;

    private byte type;

    private long id;

    private byte[] data;
}
