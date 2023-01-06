package com.cs.rpc.config;

import lombok.Data;

/**
 * @author ：cs
 * @description：配置信息
 * @date ：2022/11/22 11:31
 */
@Data
public class CsRpcConfig {
    private String nacosHost = "localhost";

    private int nacosPort = 8848;

    private int providerPort = 13567;
    /**
     * 同一个组内 互通，并组成集群
     */
//    private String nacosGroup = "cs-rpc-group";
    private String nacosGroup = "cs-rpc";
}
