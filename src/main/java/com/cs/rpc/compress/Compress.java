package com.cs.rpc.compress;

/**
 * 压缩的接口,用于帮助实现压缩和解压功能
 */


public interface Compress {
    /**
     * 压缩方法名称
     * @return
     */
     String name();
    /**
     * 压缩
     * @param bytes
     * @return
     */
    byte[] compress(byte[] bytes);

    /**
     * 解压缩
     * @param bytes
     * @return
     */
    byte[] decompress(byte[] bytes);
}
