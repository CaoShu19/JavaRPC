package com.cs.rpc.exception;

/**
 * @author ：cs
 * @description：自定义的异常
 * @date ：2022/11/19 3:16
 */
public class CsRpcException extends RuntimeException{


    public CsRpcException(String msg){
        super(msg);
    }
    public CsRpcException(String msg,Exception e){
        super(msg,e);
    }

}
