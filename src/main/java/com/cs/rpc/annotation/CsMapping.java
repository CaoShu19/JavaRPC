package com.cs.rpc.annotation;

/**
 * 此注解的作用:
 *      将网络调用的信息,传入对应的方法,并实现和运行此方法
 */

import com.cs.rpc.bean.CsBeanDefinitionRegistry;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CsMapping {
    //调用接口API路径
    String api() default "";
    //调用的主机和端口
    String url() default "";
}
