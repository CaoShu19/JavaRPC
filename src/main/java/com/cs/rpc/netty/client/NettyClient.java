package com.cs.rpc.netty.client;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.common.utils.RandomUtils;
import com.cs.rpc.config.CsRpcConfig;
import com.cs.rpc.constant.CompressTypeEnum;
import com.cs.rpc.constant.MessageTypeEnum;
import com.cs.rpc.constant.SerializationTypeEnum;
import com.cs.rpc.exception.CsRpcException;
import com.cs.rpc.factory.SingletonFactory;
import com.cs.rpc.message.CsMessage;
import com.cs.rpc.message.CsRequest;
import com.cs.rpc.message.CsResponse;
import com.cs.rpc.nacos.NacosTemplate;
import com.cs.rpc.netty.client.cache.ChannelCache;
import com.cs.rpc.netty.client.handler.CsNettyClientHandler;
import com.cs.rpc.netty.client.idle.ConnectionWatchdog;
import com.cs.rpc.netty.codec.CsRpcDecoder;
import com.cs.rpc.netty.codec.CsRpcEncoder;
import com.cs.rpc.netty.handler.UnprocessedRequests;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author ：cs
 * @description：Netty客户端
 * @date ：2022/11/21 17:30
 */
@Slf4j
public class NettyClient implements CsClient{

    /**
     * 导入配置
     */
    private CsRpcConfig csRpcConfig;

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final UnprocessedRequests unprocessedRequests;
    private final NacosTemplate nacosTemplate;
    /**
     * 用于存储实例缓存 用字符串ip,port格式存储
     */
    private final static Set<String> SERVICES = new CopyOnWriteArraySet<>();

    private final ChannelCache channelCache;

    protected final HashedWheelTimer timer = new HashedWheelTimer();

    public void setCsRpcConfig(CsRpcConfig csRpcConfig){
        this.csRpcConfig = csRpcConfig;
    }

    public CsRpcConfig getCsRpcConfig() {
        return csRpcConfig;
    }

    /**
     * 创建netty消费端
     */
    public NettyClient(){
        this.channelCache = SingletonFactory.getInstance(ChannelCache.class);
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();


        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                //超时时间设置
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5000);
    }

    @Override
    public Object sendRequest(CsRequest csRequest) throws ExecutionException, InterruptedException {
        //判断配置是否写入
        if(csRpcConfig == null){
            throw new CsRpcException("EnableRPC未被配置或开启");
        }

        //创建一个CsResponse<Object>异步线程任务管理器resultCompletableFuture
        CompletableFuture<CsResponse<Object>> resultCompletableFuture = new CompletableFuture<>();

        //1.拿到channel连接信息

        //Doubbo 的rpc :注册中心挂掉后,应用仍能运行,因为第一次获取实例后,将实例放入缓存了,可以实现降低网络的消耗
        InetSocketAddress inetSocketAddress = null;
        String ipPort = null;
        //从缓存中拿到实例
        if(!SERVICES.isEmpty()){
            //随机的负载均衡算法
            int size = SERVICES.size();
            int i = RandomUtils.nextInt(0, size - 1);
            Optional<String> optional = SERVICES.stream().skip(i).findFirst();
            if(optional.isPresent()){
                //实例字符串是否为空,能否拿到
                ipPort = optional.get();
                inetSocketAddress = new InetSocketAddress(ipPort.split(",")[0],Integer.parseInt(ipPort.split(",")[1]));
                log.info("从缓存中拿到了实例,省去");
            }
        }

        //从nacos中获取服务提供方的ip和端口
        String serviceName = csRequest.getInterfaceName()+csRequest.getVersion();

        Instance oneHealthyInstance = null;
        try {
            log.info("调用实例:"+csRpcConfig.getNacosGroup()+":"+serviceName);
            //从nacos中拿到实例
            oneHealthyInstance = nacosTemplate.getOneHealthyInstance(csRpcConfig.getNacosGroup(),serviceName);
            //拿到channel连接信息
            inetSocketAddress = new InetSocketAddress(oneHealthyInstance.getIp(), oneHealthyInstance.getPort());
            //将实例放入缓存
            ipPort = oneHealthyInstance.getIp()+","+oneHealthyInstance.getPort();
            SERVICES.add(ipPort);
        } catch (Exception e) {
            log.error("获取nacos实例出错",e);
            resultCompletableFuture.completeExceptionally(e);
            return resultCompletableFuture;
        }

        String host = oneHealthyInstance.getIp();
        int port = oneHealthyInstance.getPort();
        log.info("将要去链接:"+host+":"+port);

        //2.创建一个channel异步线程任务管理器channelCompletableFuture
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();

        //创建一个链路检测狗
        ConnectionWatchdog watchdog = new ConnectionWatchdog(bootstrap,timer,inetSocketAddress,completableFuture,true,channelCache) {
            @Override
            public ChannelHandler[] handlers() {
                return new ChannelHandler[]{
                        this,
                        new IdleStateHandler(0, 3, 0, TimeUnit.SECONDS),
                        new CsRpcDecoder(),
                        new CsRpcEncoder(),
                        new CsNettyClientHandler()
                };
            }

            @Override
            public void clear(InetSocketAddress inetSocketAddress) {
                SERVICES.remove(inetSocketAddress.getHostName()+","+inetSocketAddress.getPort());
                log.info("重连12次后,进行实例缓存清除");
            }
        };
        //给netty设置处理器
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(watchdog.handlers());
            }
        });


        String finalIpPort = ipPort;


        //5.将未处理的request的channel任务 放入缓存中
        unprocessedRequests.put(csRequest.getRequestId(),resultCompletableFuture);


        Channel channel = null;
        //6.拿到异步任务管理器中的channel
        //  此过程是阻塞的
        channel = getChannel(inetSocketAddress,completableFuture);
        if(!channel.isActive()){
            throw new CsRpcException("通道连接异常");
        }

        //7.构建发送信息
        CsMessage csMessage = CsMessage.builder()
                .codec(SerializationTypeEnum.PROTO_STUFF.getCode())
                .compress(CompressTypeEnum.GZIP.getCode())
                .messageType(MessageTypeEnum.REQUEST.getCode())
                .data(csRequest)
                .build();

        //8.将信息写入channel通道中
        // 并且添加监听器,如果写入成功,将打印日志
        channel.writeAndFlush(csMessage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()){
                    log.info(csMessage.getData()+":请求完成");
                }else {
                    log.info("发送请求数据失败");
                    //将通道关闭
                    future.channel().close();
                    //通道抛出异常
                    resultCompletableFuture.completeExceptionally(future.cause());
                }
            }
        });


        return resultCompletableFuture;
    }

    /**
     * 获得连接通道
     * @param inetSocketAddress
     * @param completableFuture
     * @return
     */
    private Channel getChannel(InetSocketAddress inetSocketAddress, CompletableFuture<Channel> completableFuture) throws ExecutionException, InterruptedException {
        Channel channel = channelCache.get(inetSocketAddress);
        if(channel == null){
            //缓存中没有可用通道,进行连接
            doConnect(inetSocketAddress,completableFuture);
            channel = completableFuture.get();
            //连接后将channel放入缓存
            channelCache.set(inetSocketAddress,channel);
            return channel;
        }else {
            log.info("channel是从缓存中获取的");
            return channel;
        }
    }

    /**
     * 进行连接
     * @param inetSocketAddress
     */
    public void doConnect(InetSocketAddress inetSocketAddress,CompletableFuture<Channel> completableFuture){
        //netty进行连接
        // 并且添加监听器,如果连接成功,将channel放入任务中
        // 此步骤是异步的:

        bootstrap.connect(inetSocketAddress).addListener(new ChannelFutureListener(){

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()){
                    //连接成功,将channel放入线程任务中
                    completableFuture.complete(future.channel());
                }else{
                    //链接失败后,服务端实例要从缓存中剔除
                    SERVICES.remove(inetSocketAddress.getHostName()+","+inetSocketAddress.getPort());
                    completableFuture.completeExceptionally(future.cause());
                    log.info("连接netty服务失败");
                }
            }
        });
    }
}
