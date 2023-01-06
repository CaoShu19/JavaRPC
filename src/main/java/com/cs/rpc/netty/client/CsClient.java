package com.cs.rpc.netty.client;

import com.cs.rpc.message.CsRequest;

import java.util.concurrent.ExecutionException;

public interface CsClient {

    Object sendRequest(CsRequest csRequest) throws ExecutionException, InterruptedException;
}
