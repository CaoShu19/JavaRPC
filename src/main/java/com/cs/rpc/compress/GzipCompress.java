package com.cs.rpc.compress;

import com.cs.rpc.constant.CompressTypeEnum;
import com.cs.rpc.exception.CsRpcException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author ：xxx
 * @description：TODO
 * @date ：2022/11/19 4:09
 */
public class GzipCompress implements Compress{
    @Override
    public String name() {
        return CompressTypeEnum.GZIP.getName();
    }

    @Override
    public byte[] compress(byte[] bytes) {
        //用java自带类实现数据压缩

        if (bytes == null){
            throw new NullPointerException("传入的压缩数据为null");
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gzip = new GZIPOutputStream(os);
            gzip.write(bytes);
            gzip.flush();
            gzip.finish();
            return os.toByteArray();
        } catch (IOException e) {
            throw new CsRpcException("压缩数据出错",e);
        }
    }

    @Override
    public byte[] decompress(byte[] bytes) {
        //用java自带的类实现数据解压

        if (bytes == null){
            throw new NullPointerException("传入的解压缩数据为null");
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes));
            byte[] buffer = new byte[1024 * 4];
            int n;
            while ((n = gzipInputStream.read(buffer)) > -1){
                os.write(buffer,0,n);
            }
            return os.toByteArray();
        } catch (IOException e) {
            throw new CsRpcException("解压缩数据出错",e);
        }
    }
}
