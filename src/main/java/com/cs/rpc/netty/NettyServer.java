package com.cs.rpc.netty;

import com.cs.rpc.netty.handler.CsRpcThreadFactory;
import com.cs.rpc.netty.handler.NettyServerInitiator;
import com.cs.rpc.server.CsServiceProvider;
import com.cs.rpc.utils.RuntimeUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ：cs
 * @description：Netty服务类
 * @date ：2022/11/17 4:06
 */
@Slf4j
public class NettyServer implements CsServer{


    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private CsServiceProvider csServiceProvider;
    private DefaultEventExecutorGroup eventExecutors;

    private boolean isRunning;

    public NettyServer(){

    }

    public void setCsServiceProvider(CsServiceProvider csServiceProvider){
        this.csServiceProvider = csServiceProvider;
    }

    @Override
    public void stopServer() {
        stopNettyServer();
        this.isRunning = false;
    }

    @Override
    public void run() {
        //主线程(main) 工作线程(work)
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        //netty服务创建
        ServerBootstrap b = new ServerBootstrap();

        //设定线程池配置:cpu和核数目*2 为线程的个数  ,用自定义的线程工厂来创建线程  并且创建线程池
        eventExecutors = new DefaultEventExecutorGroup(RuntimeUtil.cpus() * 2,new CsRpcThreadFactory(csServiceProvider));

        try {
            //配置netty服务
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY,true)
                    //是否开启 TCP 底层心跳机制 SO_KEEPALIVE保活机制打开
                    .childOption(ChannelOption.SO_KEEPALIVE,true)
                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .childOption(ChannelOption.SO_BACKLOG,1024)
                    //打印info级别的日志
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 当客户端第一次进行请求的时候才会进行初始化
                    // 将自定义的线程池配置进入netty服务
                    .childHandler(new NettyServerInitiator(eventExecutors));

            // 绑定端口，同步等待绑定成功
            b.bind(csServiceProvider.getCsRpcConfig().getProviderPort()).sync().channel();
            isRunning = true;
            //在运行时,添加一个hook(钩子):当出现shutdown事件时,就会触发内部的操作
            Runtime.getRuntime().addShutdownHook(new Thread(){
                @Override
                public void run() {
                    //将线程池给关闭等后续维护操作,关闭服务后,清空系统资源占领
                    stopServer();
                }
            });
        }catch (InterruptedException e){
            log.error("netty server 启动异常",e);
        }



    }

    private void stopNettyServer() {
        if(eventExecutors != null){
            eventExecutors.shutdownGracefully();
        }
        if(bossGroup != null){
            bossGroup.shutdownGracefully();
        }
        if(workerGroup != null){
            workerGroup.shutdownGracefully();
        }
    }


    public boolean isRunning() {
        return isRunning;
    }
}
