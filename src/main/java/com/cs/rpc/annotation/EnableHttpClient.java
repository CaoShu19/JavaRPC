package com.cs.rpc.annotation;

import com.cs.rpc.bean.CsBeanDefinitionRegistry;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 此注解的作用:
 *      让能够其它包类可以使用指定包中的注解
 *      让扫描器可以扫描到指定的包
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CsBeanDefinitionRegistry.class)//将Bean注册器引入
public @interface EnableHttpClient {
    //扫描包的路径
    String basePackage();
}
