package com.cs.rpc.spring;

import com.cs.rpc.annotation.CsReference;
import com.cs.rpc.annotation.CsService;
import com.cs.rpc.annotation.EnableRpc;
import com.cs.rpc.config.CsRpcConfig;
import com.cs.rpc.factory.SingletonFactory;
import com.cs.rpc.nacos.NacosTemplate;
import com.cs.rpc.netty.client.NettyClient;
import com.cs.rpc.proxy.CsRpcClientProxy;
import com.cs.rpc.server.CsServiceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author ：cs
 * @description：此类用于实现@CsService注解的作用
 * @date ：2022/11/17 2:18
 */

/**
 *  BeanPostProcessor这个接口是spring提供用于控制bean的生命周期的中的行为
 *      此接口中的方法会在Bean初始化前或后进行调用,当然一般自定义操作会写到初始化后
 */
@Slf4j
public class CsRpcSpringBeanPostProcessor implements BeanPostProcessor, BeanFactoryPostProcessor {

    /**
     * 在bean处理类中添加一个服务提供类,帮助实现服务发布业务
     */
    private CsServiceProvider csServiceProvider;
    /**
     * 在bean处理类中添加一个客户处理类,帮助实现客户端调用
     */
    private NettyClient nettyClient;

    /**
     * RPC相关的配置类
     */
    private CsRpcConfig csRpcConfig;
    /**
     * 将Nacos提前配置好
     */
    private NacosTemplate nacosTemplate;

    public CsRpcSpringBeanPostProcessor(){
        //由于是自定义的服务提供类,需要用import注解引入到spring中,但每次import引用都会创建一个对象
        //每次的创建一个对象,成员变量都会不一样,所以我们不用普通的new来进行实例化,而是用工厂模式创造单例对象
        //防止线程冲突,同时便于其它类使用
        csServiceProvider = SingletonFactory.getInstance(CsServiceProvider.class);
        nettyClient = SingletonFactory.getInstance(NettyClient.class);
        nacosTemplate = SingletonFactory.getInstance(NacosTemplate.class);
    }

    /**
     * bean初始化之前调用
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        //postProcessBeanFactory()此方法会先执行,得到注解信息的bean会传入当前方法中 (bean的生命周期)
        EnableRpc enableRpc = bean.getClass().getAnnotation(EnableRpc.class);
        if(enableRpc != null){
            if(csRpcConfig == null){
                log.info("EnableRpc添加在 "+bean.getClass().getName()+" 上");
                log.info("EnableRpc,会先于所有的bean实例化前 执行");
                csRpcConfig = new CsRpcConfig();
                csRpcConfig.setNacosGroup(enableRpc.nacosGroup());
                csRpcConfig.setNacosHost(enableRpc.nacosHost());
                csRpcConfig.setProviderPort(enableRpc.serverPort());
                csRpcConfig.setNacosPort(enableRpc.nacosPort());
                //将配置写入消费端,服务提供端 以及nacos服务端
                nettyClient.setCsRpcConfig(csRpcConfig);
                csServiceProvider.setCsRpcConfig(csRpcConfig);
                //初始化nacos服务
                nacosTemplate.init(csRpcConfig.getNacosHost(),csRpcConfig.getNacosPort());
            }
        }

        return bean;
    }

    /**
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        //参数bean代表的是spring中所有扫描到的bean
        //首先:找到CsService注解和CsReference注解
        //System.out.println(bean);
        if(bean.getClass().isAnnotationPresent(CsService.class)){
            //如果存在有添加了CsService注解的类,那么找到此类
            CsService csService = bean.getClass().getAnnotation(CsService.class);
            //找到了加了CsService注解的的bean,把bean里面的方法发布为TCP服务
            //发布服务
            csServiceProvider.publishService(csService,bean);

            log.info(bean.getClass().getName()+"添加了csService注解");
        }
        //获得所有属性数组
        Field[] declaredFields = bean.getClass().getDeclaredFields();
        //去找加了CsReference注解的属性
        for (Field declaredField : declaredFields) {
            CsReference csReference = declaredField.getAnnotation(CsReference.class);
            if(csReference != null){
                //找到了需要调用tcp服务的接口,因为某接口对象上添加了CsReference注解
                //由于是个接口,想要实现接口的作用,那么需要生成一个代理类来代理接口对象完成任务
                //当接口的方法调用的时候,实际上是访问代理类中的invoke方法

                //创建代理类
                CsRpcClientProxy csRpcClientProxy = new CsRpcClientProxy(csReference,nettyClient);
                //以添加了CsReference注解的接口类型作为参数 调用内部方法 :通过接口类来生成代理对象
                Object proxy = csRpcClientProxy.getProxy(declaredField.getType());

                //当isAccessible()的结果是false时不允许通过反射访问该字段
                //开启属性可以通过反射获取
                declaredField.setAccessible(true);
                //将添加有CsReference注解的属性成员bean对象改为proxy,其中proxy被set注入到类的成员中
                try {
                    declaredField.set(bean,proxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return bean;
    }

    /**
     * 将加载所有的bean定义，但是还没有实例化任何bean，在这里你可以定义新的bean，
     *  也可以修改bean的一些属性或者覆盖一些属性
     *  BeanFactoryPostProcessor的执行顺序是早于BeanPostProcessor，
     *  也就是说我们需要在bean初始化之前，我们先扫描到@EnableRpc注解，拿到我们做的RPC相关的配置
     */

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                // init scanner
                Class<?> scannerClass = ClassUtils.forName ( "org.springframework.context.annotation.ClassPathBeanDefinitionScanner",
                        CsRpcSpringBeanPostProcessor.class.getClassLoader () );
                Object scanner = scannerClass.getConstructor ( new Class<?>[]{BeanDefinitionRegistry.class, boolean.class} )
                        .newInstance ( new Object[]{(BeanDefinitionRegistry) beanFactory, true} );
                // add filter
                Class<?> filterClass = ClassUtils.forName ( "org.springframework.core.type.filter.AnnotationTypeFilter",
                        CsRpcSpringBeanPostProcessor.class.getClassLoader () );
                Object filter = filterClass.getConstructor ( Class.class ).newInstance ( EnableRpc.class );
                Method addIncludeFilter = scannerClass.getMethod ( "addIncludeFilter",
                        ClassUtils.forName ( "org.springframework.core.type.filter.TypeFilter", CsRpcSpringBeanPostProcessor.class.getClassLoader () ) );
                addIncludeFilter.invoke ( scanner, filter );
                // scan packages
                Method scan = scannerClass.getMethod ( "scan", new Class<?>[]{String[].class} );
                scan.invoke ( scanner, new Object[]{"com.cs.rpc.annotation"} );
            } catch (Throwable e) {
                // spring 2.0
            }
        }

    }
}
