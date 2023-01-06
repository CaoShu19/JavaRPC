package com.cs.rpc.bean;

import com.cs.rpc.proxy.CsHttpClientProxy;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author ：cs
 * @description：通过实现FactoryBean完成对CsHttpClient注解的实例化BEan生产
 * @date ：2022/11/15 0:50
 */
public class CsHttpClientFactoryBean<T> implements FactoryBean<T> {

    private Class<T> interfaceClass;

    /**
     * builder会按照getObject()方法进行构建
     */
    @Override
    public T getObject() throws Exception {
        //返回一个代理实现类,这个代理类会帮助使用注解的功能(注解被代理了)
        return new CsHttpClientProxy().getProxy(this.interfaceClass);
    }

    //类型是接口
    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }


    public Class<T> getInterfaceClass(){
        return interfaceClass;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }
}
