package com.cs.rpc.server;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.cs.rpc.annotation.CsService;
import com.cs.rpc.config.CsRpcConfig;
import com.cs.rpc.exception.CsRpcException;
import com.cs.rpc.factory.SingletonFactory;
import com.cs.rpc.nacos.NacosTemplate;
import com.cs.rpc.netty.NettyServer;
import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：cs
 * @description：服务提供类
 * @date ：2022/11/17 3:12
 */
@Slf4j
public class CsServiceProvider {

    private CsRpcConfig csRpcConfig;
    private final Map<String,Object> serviceMap;
    private NacosTemplate nacosTemplate;

    public void setCsRpcConfig(CsRpcConfig csRpcConfig) {
        this.csRpcConfig = csRpcConfig;
    }

    public CsRpcConfig getCsRpcConfig() {
        return csRpcConfig;
    }

    public CsServiceProvider(){
        serviceMap = new ConcurrentHashMap<>();
        nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
    }

    public void publishService(CsService csService, Object service) {
        //从bean中获得到服务的名字

        //从要创建的Bean中获得上面的接口,获得第一个接口===>接口数组
        String canonicalName = service.getClass().getInterfaces()[0].getCanonicalName();


        log.info(canonicalName+":已经注册到provider中");
        //将服务注册在provider中,serviceProvider已经记录了服务信息
        registerService(csService,service);


        //启动nettyServer 启动单例的服务即可
        NettyServer nettyServer = SingletonFactory.getInstance(NettyServer.class);
        nettyServer.setCsServiceProvider(this);
        //只能调用一次,所以需要判断是否已经启动
        if(!nettyServer.isRunning()){
            nettyServer.run();
        }

    }

    private void registerService(CsService csService, Object service) {
        //获得注解上的版本
        String version = csService.version();
        //获得注解标记的服务类的信息
        String interfaceName = service.getClass().getInterfaces()[0].getCanonicalName();
        //记录一下日志
        log.info("发布了服务:{}",interfaceName);
        //将服务已经放入到CsServiceProvider的map中,provide已经记录了
        serviceMap.put(interfaceName+version,service);
        //判断配置是否写入
        if(csRpcConfig == null){
            throw new CsRpcException("EnableRPC未被配置或开启");
        }

        try{
            //同步注册到Nacos中
            //group:Nacos中提供的组,意思是只有同一个组类,调用关系才成立,不同组之间是隔离de
            //1.先将实例创建好放入组cs-rpc(group)中
            Instance instance = new Instance();
            //将服务发布服务器本机Ip写到实例中
            instance.setIp(InetAddress.getLocalHost().getHostAddress());
            //将服务运行的端口写到实例中
            instance.setPort(csRpcConfig.getProviderPort());
            //将集群名称写到实例中
            instance.setClusterName("cs-rpc");
            //将服务名和版本写道实例中
            instance.setServiceName(interfaceName+version);
            //将实例注册到nacos
            nacosTemplate.registerServer(csRpcConfig.getNacosGroup(),instance);

        }catch (Exception e){
            log.error("nacos注册失败",e);
        }

    }

    public Object getService(String serviceName){
        return serviceMap.get(serviceName);
    }


}
