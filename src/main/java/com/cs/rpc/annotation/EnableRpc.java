package com.cs.rpc.annotation;

import com.cs.rpc.bean.CsBeanDefinitionRegistry;
import com.cs.rpc.spring.CsRpcSpringBeanPostProcessor;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 此注解的作用:
 *      让能够其它包类可以使用指定包中的注解
 *      让扫描器可以扫描到指定的包
 * @author Acos
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CsRpcSpringBeanPostProcessor.class)//将Bean处理器(CsRpcSpringBeanPostProcessor)引入容器中
@Inherited
public @interface EnableRpc {
    //nacos主机名
    String nacosHost() default "localhost";
    //nacos端口号
    int nacosPort() default 8848;

    //nacos组，同一个组内 互通，并且组成集群
    //String nacosGroup() default "cs-rpc-group";
    String nacosGroup() default "cs-rpc";

    //server服务端口
    int serverPort() default 13567;

}
