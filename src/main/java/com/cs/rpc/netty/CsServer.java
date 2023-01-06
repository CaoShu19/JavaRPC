package com.cs.rpc.netty;

/**
 * 此接口的作用:
 *  服务的实现不止一种,
 *  可以通过不同的实现方式,实现选择不同的远程调用服务
 */

public interface CsServer {
    void run();

    void stopServer();
}
