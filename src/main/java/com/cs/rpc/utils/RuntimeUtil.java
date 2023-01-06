package com.cs.rpc.utils;

/**
 * @author ：cs
 * @description：运行工具
 * @date ：2022/11/17 5:02
 */
public class RuntimeUtil {
    /**
     * 返回电脑的CPU核心数目
     * @return
     */
    public static int cpus(){
        return Runtime.getRuntime().availableProcessors();
    }

}
