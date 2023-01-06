package com.cs.rpc.netty.client.idle;

import java.net.InetSocketAddress;

/**
 * @author Acos
 * 自定义的接口,用于清除缓存
 */
public interface CacheClearHandler {
    void clear(InetSocketAddress inetSocketAddress);
}
