package com.cs.rpc.factory;

import com.cs.rpc.server.CsServiceProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 获取单例对象的工厂类
 *
 */
public final class SingletonFactory {
    /**
     * 为什么指向本身实例的类属性为静态？
     * 因为指向自己实例的私有引用在被类方法(Getinstance)调用时被初始化，
     * 只有静态成员变量才能在没有创建对象时进行初始化，
     * 并且类的静态成员在第一次使用时不会再被初始化，
     * 保证了单例，因此设置为静态。
     */

    private static final Map<String, Object> OBJECT_MAP = new ConcurrentHashMap<>();

    /**
     * 将该类的构造函数私有化，目的是禁止其他程序创建该类的对象，
     * 同时也是为了提醒查看代码的人我这里是在使用单例模式，
     * 防止他人将这里任意修改。
     * 此时，需要提供一个可访问类自定义对象的类成员方法（
     * 对外提供该对象的访问方式）
     */
    private SingletonFactory() {
    }

    /**
     * 为什么以自己实例为返回值的类方法为静态？
     * 通过类方法（GetInstance) 获取instance，类属性instance为静态的(static)，
     * 则需要类的静态方法才能调用，因此该类方法应设为静态的
     */
    public static <T> T getInstance(Class<T> c) {
        if (c == null) {
            throw new IllegalArgumentException();
        }
        String key = c.toString();
        if (OBJECT_MAP.containsKey(key)) {
            return c.cast(OBJECT_MAP.get(key));
        } else {
            return c.cast(OBJECT_MAP.computeIfAbsent(key, k -> {
                try {
                    return c.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage(), e);
                }
            }));
        }
    }

    public static void main(String[] args) {
        //测试并发下 生成的单例是否唯一
        ExecutorService executorService = Executors.newFixedThreadPool(100);

        for (int i = 0 ; i< 1000; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    CsServiceProvider instance = SingletonFactory.getInstance(CsServiceProvider.class);
                    System.out.println(instance);
                }
            });
        }
        while (true){}
    }
}
