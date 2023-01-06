package com.cs.rpc.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Acos
 */

@AllArgsConstructor
@Getter
public enum MessageTypeEnum {
    //信息类型 整形表示 对应 信息类型的mingcheng
    REQUEST((byte) 0x01, "request"),
    RESPONSE((byte) 0x02, "response"),
    HEARTBEAT_PING((byte) 0x03, "heart ping"),
    HEARTBEAT_PONG((byte) 0x04, "heart pong");

    private final byte code;
    private final String name;

    public static String getName(byte code) {
        for (MessageTypeEnum c : MessageTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}
