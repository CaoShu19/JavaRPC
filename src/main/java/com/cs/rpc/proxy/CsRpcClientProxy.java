package com.cs.rpc.proxy;

import com.cs.rpc.annotation.CsMapping;
import com.cs.rpc.annotation.CsReference;
import com.cs.rpc.exception.CsRpcException;
import com.cs.rpc.factory.SingletonFactory;
import com.cs.rpc.message.CsRequest;
import com.cs.rpc.message.CsResponse;
import com.cs.rpc.netty.client.NettyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ：cs
 * @description：代理一个接口,并完成对应任务
 * @date ：2022/11/15 1:22
 */

/**
 * //每一个动态代理类的调用处理程序都必须实现InvocationHandler接口，
 * // 并且每个代理类的实例都关联到了实现该接口的动态代理类调用处理程序中，
 * // 当我们通过动态代理对象调用一个方法时候，
 * // 这个方法的调用就会被转发到实现InvocationHandler接口类的invoke方法来调用
 * @author Acos
 */
@Slf4j
public class CsRpcClientProxy implements InvocationHandler {
    private CsReference csReference;
    private NettyClient nettyClient;


    public CsRpcClientProxy(){

    }

    public CsRpcClientProxy(CsReference csReference,NettyClient nettyClient){
        this.csReference = csReference;
        this.nettyClient = nettyClient;
    }

    /**
     *当接口的注解添加上时,实际上是代理类的invoke方法调用了
     * //我们只要直接实现invoke方法,那么就可以自定义代理的对象能够做什么了
     */
    @Override
    public Object invoke(Object o, Method method, Object[] args) throws Throwable {
        //客服端,消费者发起请求
        log.info("rpc 服务消费方 发起调用,invoke被激活");

        //对invoke进行实现:发送调用服务请求

        //1. 构建请求数据MsRequest
        //2. 调用Netty客户端
        //3. 通过客户端向服务端发送请求
        //4. 接收数据

        //拿到注解中信息
        //String host = csReference.host();//已经用nacos进行服务注册
        //int port = csReference.port();
        String version = csReference.version();

        log.info(method+":此方法已经被代理实现");
        //构建一个请求
        String requestId = UUID.randomUUID().toString();
        CsRequest csRequest = CsRequest.builder()
                .group("cs-rpc")
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .version(version)
                .parameters(args)
                .paramTypes(method.getParameterTypes())
                .requestId(requestId)
                .build();


        //通过客户端向服务端发送请求,然后返回一个response结果管理线程池
        Object sendRequest = nettyClient.sendRequest(csRequest);
        //强转一下,方便使用
        CompletableFuture<CsResponse<Object>> resultCompletableFuture = (CompletableFuture<CsResponse<Object>>)sendRequest;
        //从结果线程池中找到服务端对应的返回
        CsResponse<Object> csResponse = resultCompletableFuture.get();


        if(csResponse == null){
            throw new CsRpcException("服务调用失败");
        }
        if(!requestId.equals(csResponse.getRequestId())){
            throw new CsRpcException("响应结果和请求不一致");
        }
        return  csResponse.getData();
    }

    /**
     * 通过接口 来生成代理类对象
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> interfaceClass){
        return (T)Proxy.newProxyInstance(interfaceClass.getClassLoader(),new Class<?>[]{interfaceClass},this);
    }
}
