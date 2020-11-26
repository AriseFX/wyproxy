package com.wy.common;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;

/**
 * @Author: wy
 * @Date: Created in 9:18 2020/11/26
 * @Description:
 * @Modified: By：
 */
public class PendingTask {

    private final Promise<Channel> promise;

    public PendingTask(Promise<Channel> promise) {
        this.promise = promise;
    }

    public Promise<Channel> getPromise() {
        return promise;
    }
}
