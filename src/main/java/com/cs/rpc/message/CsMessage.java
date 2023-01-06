package com.cs.rpc.message;


import lombok.*;

/**
 * 将数据流反序列化后需要类对象来承接
 * CsMassage就是信息类
 */

@AllArgsConstructor
@NoArgsConstructor
@Builder //可以通过builder方法构建对象(构建者模式)
@Data
public class CsMessage {

    //rpc message type
    private byte messageType;
    //serialization type
    private byte codec;
    //compress type
    private byte compress;
    //request id
    private int requestId;
    //request data
    private Object data;

}
