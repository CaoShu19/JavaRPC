package com.cs.rpc.netty.client.idle;

import com.cs.rpc.netty.client.cache.ChannelCache;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable//用来说明ChannelHandler是否可以在多个channel直接共享使用
@Slf4j
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask,ChannelHandlerHolder,CacheClearHandler{

    private final Bootstrap bootstrap;
    private final Timer timer;
    private final InetSocketAddress inetSocketAddress;

    private volatile boolean reconnect = true;
    private int attempts;

    private final CompletableFuture<Channel> completableFuture;
    private final ChannelCache channelCache;


    public ConnectionWatchdog(Bootstrap bootstrap,
                              Timer timer,
                              InetSocketAddress inetSocketAddress,
                              CompletableFuture<Channel> completableFuture,
                              boolean b,
                              ChannelCache channelCache) {
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.inetSocketAddress = inetSocketAddress;
        this.reconnect = reconnect;
        this.completableFuture = completableFuture;
        this.channelCache = channelCache;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("连接存活");
        //表示连接状体啊
        super.channelActive(ctx);
        attempts = 0;
        ctx.fireChannelActive();

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("连接关闭");;
        //代表未连接,此时应该发生重试连接策略
        super.channelInactive(ctx);
        if(reconnect){
            log.info("连接关闭,将进行重连");
            //是否重连
            if(attempts < 12){
                attempts++;
                log.info("重连次数:{}",attempts);
            }else {
                //不重连,清除缓存
                reconnect = false;
                clear(inetSocketAddress);
            }
            //时间每次乘以2
            int timeout = 2 << attempts;
            //定时任务执行
            timer.newTimeout(this,timeout, TimeUnit.SECONDS);
        }
    }

    @Override
    public void run(Timeout timeout) throws Exception {
        ChannelFuture future = null;
        synchronized (bootstrap){
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(handlers());
                }
            });
            future = bootstrap.connect(inetSocketAddress);
        }

        //定时任务执行
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()){
                    //重连成功
                    completableFuture.complete(future.channel());
                    //重连成功,链路激活放入
                    channelCache.set(inetSocketAddress,future.channel());
                }else {
                    future.channel().pipeline().fireChannelInactive();
                }
            }
        });
    }
}
