package com.cs.rpc.netty.handler;


import com.cs.rpc.netty.codec.CsRpcDecoder;
import com.cs.rpc.netty.codec.CsRpcEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.util.concurrent.TimeUnit;

/**
 * @author ：cs
 * @description：传入自定义线程池 用于 netty服务初始化
 * @date ：2022/11/17 5:05
 */
public class NettyServerInitiator extends ChannelInitializer<SocketChannel> {

    private DefaultEventExecutorGroup eventExecutors;

    public NettyServerInitiator(DefaultEventExecutorGroup eventExecutors){
        //初始化线程池
        this.eventExecutors = eventExecutors;
    }
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        //自定义TCP协议  定义数据报文格式 能后解决粘包和拆包问题
        //使用TCP的方式，那么当发起一个网络请求的时候，势必要传递数据流，
        //  那么这个数据流的格式是什么，我们就需要进行定义了，我们可以称为`自定义报文`或者`自定义协议`

        //添加心跳检测处理器: 10s没有读操作,那么触发心跳检测事件(需要我们去实现)
        ch.pipeline().addLast(new IdleStateHandler(10,0,0, TimeUnit.SECONDS));
        //解码器 接收到数据流后，按照自定义的协议进行解析(负责将消息从字节或其他序列形式转成指定的消息对象)
        ch.pipeline ().addLast ( "decoder",new CsRpcDecoder() );
        //编码器 发送数据的时候，按照自定义的协议进行构建并发送(将消息对象转成字节或其他序列形式在网络上传输)
        ch.pipeline ().addLast ( "encoder",new CsRpcEncoder());
        //消息处理器，线程池处理
        ch.pipeline ().addLast ( eventExecutors,"handler",new CsNettyServerHandler());
    }
}
