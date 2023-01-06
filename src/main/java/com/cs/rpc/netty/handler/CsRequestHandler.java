package com.cs.rpc.netty.handler;

import com.cs.rpc.exception.CsRpcException;
import com.cs.rpc.factory.SingletonFactory;
import com.cs.rpc.message.CsRequest;
import com.cs.rpc.server.CsServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author ：cs
 * @description：请求处理
 * @date ：2022/11/21 15:57
 */
@Slf4j
public class CsRequestHandler {
    private CsServiceProvider csServiceProvider;

    public CsRequestHandler(){
        csServiceProvider = SingletonFactory.getInstance(CsServiceProvider.class);
    }

    public Object handler(CsRequest csRequest) {
        //请求处理器,去处理数据

        //从请求中获得请求的接口和版本
        String interfaceName = csRequest.getInterfaceName();
        String version = csRequest.getVersion();

        //验证此请求是否已经注册上
        Object service = csServiceProvider.getService(interfaceName + version);
        if(service == null){
            throw new CsRpcException("没有找到可用服务提供方");
        }

        try{
            //获得TCP请求中的调用的方法和参数
            Method method = service.getClass().getMethod(csRequest.getMethodName(), csRequest.getParamTypes());

            Object invoke = method.invoke(service, csRequest.getParameters());
            log.info(invoke+":请求处理得到一个invoke.传入的请求中的方法"+csRequest.getMethodName()+",参数为"+csRequest.getParamTypes());
            return invoke;
        }catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            log.info("服务提供方,方法调用提供问题:",e);
        }
        return null;
    }
}
