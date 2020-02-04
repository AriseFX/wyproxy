package com.wy.common;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import io.netty.channel.Channel;


/**
 * @Author: wy
 * @Date: Created in 19:04 2020/1/30
 * @Description: Channel管理容器
 * @Modified: By：
 */
public class ChannelContainer {

    public static BiMap<String, Channel> container = Maps.synchronizedBiMap(HashBiMap.create());

}
