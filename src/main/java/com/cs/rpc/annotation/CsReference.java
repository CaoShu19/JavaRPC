package com.cs.rpc.annotation;


import com.cs.rpc.spring.CsRpcSpringBeanPostProcessor;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.FIELD,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({CsRpcSpringBeanPostProcessor.class})
public @interface CsReference {

//    //netty服务主机名
//    String host();
//    //netty服务端口号
//    int port();

   String version() default "1.0";


}
