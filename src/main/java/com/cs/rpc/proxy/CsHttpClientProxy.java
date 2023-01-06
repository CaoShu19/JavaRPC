package com.cs.rpc.proxy;

import com.cs.rpc.annotation.CsMapping;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ：cs
 * @description：代理一个接口,并完成对应任务
 * @date ：2022/11/15 1:22
 */

/**
 * //每一个动态代理类的调用处理程序都必须实现InvocationHandler接口，
 * // 并且每个代理类的实例都关联到了实现该接口的动态代理类调用处理程序中，
 * // 当我们通过动态代理对象调用一个方法时候，
 * // 这个方法的调用就会被转发到实现InvocationHandler接口类的invoke方法来调用
 */
public class CsHttpClientProxy implements InvocationHandler {

    public CsHttpClientProxy(){

    }

    /**
     *当接口的注解添加上时,实际上是代理类的invoke方法调用了
     * //我们只要直接实现invoke方法,那么就可以自定义代理的对象能够做什么了
     */
    @Override
    public Object invoke(Object o, Method method, Object[] args) throws Throwable {
        //实现向服务提供方调用用
        System.out.println("网络查询服务被调用了");
        //代理类的方法一样能够拿到 就是 method
        //通过method获得上面的注解
        CsMapping annotation = method.getAnnotation(CsMapping.class);
        if(annotation != null){
            //获得方法上注解的参数值
            // ip:port/api/{index}
            String url = annotation.url();
            String api = annotation.api();
            //获取index
            //设定正则表达式 去单独获得参数:去匹配路径中的大括号
            Pattern compile = Pattern.compile("(\\{\\w+})");
            //利用正则表达式去匹配API中的参数
            Matcher matcher = compile.matcher(api);
            if(matcher.find()){//匹配到了,然后对API进行替换
                int count = matcher.groupCount();
                for (int i = 0; i < count; i++) {
                    String group = matcher.group(i);
                    api = api.replace(group, args[i].toString());
                }
            }
            //调用远程服务
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.getForObject(url + api, method.getReturnType());
        }

        return null;
    }

    /**
     * 通过接口 来生成代理类对象
     * @return
     */
    public <T> T getProxy(Class<T> interfaceClass){
        return (T)Proxy.newProxyInstance(interfaceClass.getClassLoader(),new Class<?>[]{interfaceClass},this);
    }
}
