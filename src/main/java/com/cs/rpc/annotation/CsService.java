package com.cs.rpc.annotation;


import java.lang.annotation.*;

/**
 * 用此注解标识需要发布的TCP服务,放在提供服务的实现类上后,此类实现服务
 * HTTP协议提供的服务,只需要暴露出接口,但TCP需要实现
 */

//可用于类上
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface CsService {

    String version() default "1.0";
}
