package com.wy.common;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @Author: wy
 * @Date: Created in 16:01 2020/11/24
 * @Description: 连接池
 * @Modified: By：
 */
public class ProxyChannelPool {

    private final int maxConnections;

    private final Bootstrap bootstrap;
    private final ProxyChannelPoolHandler poolHandler;
    private final EventLoop eventLoop;

    private final AtomicInteger acquiredChannelCount = new AtomicInteger();
    private int pendingChannelCount = 0;

    private final Queue<PendingTask> pendingAcquireQueue = new ArrayDeque<>();
    private final Deque<Channel> deque = new ConcurrentLinkedDeque<>();

    private String host;
    private int port;

    public ProxyChannelPool(Bootstrap bootstrap, ProxyChannelPoolHandler poolHandler, int maxConnections, String host, int port) {
        this.bootstrap = bootstrap;
        this.poolHandler = poolHandler;
        this.maxConnections = maxConnections;
        this.host = host;
        this.port = port;
        eventLoop = bootstrap.config().group().next();
    }

    /**
     * 获取channel
     *
     * @return
     */
    public Promise<Channel> getChannel() {
        Promise<Channel> channelPromise = bootstrap
                .config().group().next().newPromise();
        //当前线程是否是当前eventLoop的线程
        if (eventLoop.inEventLoop()) {
            getChannel0(channelPromise);
        } else {
            eventLoop.execute(() ->
                    getChannel0(channelPromise));
        }
        return channelPromise;
    }

    public ChannelFuture getNewChannel() {
        return bootstrap.connect(host, port);
    }

    /**
     * 释放channel
     */
    public Future<Channel> release(final Channel channel) {
        Promise<Channel> promise = bootstrap
                .config().group().next().newPromise();
        try {
            poolHandler.channelReleased(channel);

            Promise<Void> p = eventLoop.newPromise();
            doRelease(channel, p.addListener((FutureListener<Void>) future -> {
                if (future.isSuccess()) {
                    promise.setSuccess(null);
                    runTaskQueue();
                } else {
                    Throwable cause = future.cause();
                    // Check if the exception was not because of we passed the Channel to the wrong pool.
                    if (!(cause instanceof IllegalArgumentException)) {
                        runTaskQueue();
                    }
                    promise.setFailure(future.cause());
                }
            }));
        } catch (Exception e) {
            channel.close();
            promise.setFailure(new IllegalStateException("call [channelReleased] fail") {
                //减少爬栈
                @Override
                public synchronized Throwable fillInStackTrace() {
                    return this;
                }
            });
        }
        return promise;
    }

    public void getChannel0(Promise<Channel> channelPromise) {
        if (acquiredChannelCount.get() < maxConnections) {
            createOrFormPool(channelPromise);
        } else {
            if (pendingChannelCount < maxConnections) {
                channelPromise.setFailure(new IllegalStateException("Too many acquire operations") {
                    //减少爬栈
                    @Override
                    public synchronized Throwable fillInStackTrace() {
                        return this;
                    }
                });
            } else {
                //create pending task
                PendingTask pt = new PendingTask(channelPromise);
                if (pendingAcquireQueue.offer(pt)) {
                    ++pendingChannelCount;
                }
            }
        }
    }


    private void runTaskQueue() {
        acquiredChannelCount.decrementAndGet();
        while (acquiredChannelCount.get() < maxConnections) {
            PendingTask task = pendingAcquireQueue.poll();
            if (task == null) {
                break;
            }
            --pendingChannelCount;
            createOrFormPool(task.getPromise());
        }
    }

    public void doRelease(final Channel channel, final Promise<Void> promise) {
        if (deque.offer(channel)) {
            promise.setSuccess(null);
        } else {
            channel.close();
            promise.tryFailure(new IllegalStateException("ChannelPool full") {
                @Override
                public synchronized Throwable fillInStackTrace() {
                    return this;
                }
            });
        }
    }


    private void createOrFormPool(Promise<Channel> channelPromise) {
        Channel channel = deque.pollFirst();
        if (channel == null) {
            ChannelFuture cf = bootstrap.connect(host, port).syncUninterruptibly();
            if (cf.isDone()) {
                connectNotify(cf, channelPromise);
            } else {
                cf.addListener(future ->
                        connectNotify(cf, channelPromise));
            }
            return;
        }
        //当前线程是否是当前eventLoop的线程
        EventLoop ep = channel.eventLoop();
        if (ep.inEventLoop()) {
            check(channel, channelPromise);
        } else {
            //队列中执行
            ep.execute(() ->
                    check(channel, channelPromise));
        }
    }

    private void connectNotify(ChannelFuture future, Promise<Channel> promise) {
        if (future.isSuccess()) {
            Channel channel = future.channel();
            if (!promise.trySuccess(channel)) {
                promise.setFailure(new IllegalAccessException());
                release(channel);
            }
        } else {
            promise.setFailure(new IllegalStateException("call [connect] fail") {
                //减少爬栈
                @Override
                public synchronized Throwable fillInStackTrace() {
                    return this;
                }
            });
        }
    }

    private void check(Channel channel, Promise<Channel> promise) {
        if (channel.isActive()) {
            promise.setSuccess(channel);
            acquiredChannelCount.incrementAndGet();
        } else {
            channel.close();
            createOrFormPool(promise);
        }
    }
}
