package com.cs.rpc.netty.handler;

import com.cs.rpc.server.CsServiceProvider;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程工厂类:创建线程,配置线程信息
 */

public class CsRpcThreadFactory implements ThreadFactory {

    private CsServiceProvider csServiceProvider;

    //线程池数目 原子类自增
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    //线程数目 原子类自增
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    //线程名前缀
    private final String namePrefix;
    //线程池组
    private final ThreadGroup threadGroup;

    public CsRpcThreadFactory(CsServiceProvider csServiceProvider) {

        this.csServiceProvider = csServiceProvider;
        SecurityManager securityManager = System.getSecurityManager();
        threadGroup = securityManager != null ? securityManager.getThreadGroup() :Thread.currentThread().getThreadGroup();
        namePrefix = "cs-rpc-" + poolNumber.getAndIncrement()+"-thread-";
    }

    //创建的线程以“N-thread-M”命名，N是该工厂的序号，M是线程号
    public Thread newThread(Runnable runnable) {
        //创建线程,每次创建完一个线程,线程数目自动加一
        Thread t = new Thread(threadGroup, runnable,
                namePrefix + threadNumber.getAndIncrement(), 0);
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
