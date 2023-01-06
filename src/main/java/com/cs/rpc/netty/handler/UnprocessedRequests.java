package com.cs.rpc.netty.handler;


import com.cs.rpc.message.CsResponse;


import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：cs
 * @description：用于存放和管理未被处理的request
 * @date ：2022/11/21 21:13
 */


public class UnprocessedRequests {
    private static final Map<String, CompletableFuture<CsResponse<Object>>> UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public UnprocessedRequests(){

    }
    public void put(String requestId, CompletableFuture<CsResponse<Object>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }

    public void complete(CsResponse<Object> rpcResponse) {
        CompletableFuture<CsResponse<Object>> future = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }
}
