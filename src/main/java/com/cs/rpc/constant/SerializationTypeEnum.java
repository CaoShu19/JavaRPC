package com.cs.rpc.constant;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SerializationTypeEnum {
    //序列化枚举,用于得到序列化类型的名字

    PROTO_STUFF((byte) 0x01, "protoStuff"),
    JSON((byte)0x02,"json");

    private final byte code;
    private final String name;

    public static String getName(byte code) {
        //从解码得到类型(int)得到压缩类型名称
        for (SerializationTypeEnum c : SerializationTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}
