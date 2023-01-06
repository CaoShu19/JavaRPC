package com.cs.rpc.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CompressTypeEnum {
	//读取协议这的压缩类型，来此枚举进行匹配
    GZIP((byte) 0x01, "gzip"),

    OTHER((byte) 0x02, "other");

    private final byte code;
    private final String name;

    public static String getName(byte code) {
        //从解码得到类型(int)得到压缩类型名称
        for (CompressTypeEnum c : CompressTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}
