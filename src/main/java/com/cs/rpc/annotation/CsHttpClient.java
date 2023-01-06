package com.cs.rpc.annotation;

import com.cs.rpc.bean.CsBeanDefinitionRegistry;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;


/**
 * 此注解的作用:
 *      将对应的接口实现,并且将接口对象实例化注入IOC容器中
 */

@Target({ElementType.TYPE})//可用于类上
@Retention(RetentionPolicy.RUNTIME)//运行时
@Documented//可文档
@Inherited//可继承
public @interface CsHttpClient {
    //必须填,代表此注解下的接口形成的对象bean的名称
    String value();
}
