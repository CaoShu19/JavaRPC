package com.cs.rpc.netty.codec;

import com.cs.rpc.compress.Compress;
import com.cs.rpc.constant.CompressTypeEnum;
import com.cs.rpc.constant.CsRpcConstants;
import com.cs.rpc.constant.SerializationTypeEnum;
import com.cs.rpc.exception.CsRpcException;
import com.cs.rpc.message.CsMessage;
import com.cs.rpc.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ：cs
 * @description：编码,Object转化为数据流(按照自定义TCP协议转码)
 * @date ：2022/11/19 3:06
 */
public class CsRpcEncoder extends MessageToByteEncoder<CsMessage> {


    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext,
                          CsMessage csMessage,
                          ByteBuf out) throws Exception {
        //获得message对象,然后编码处理,编码为二进制流

        //对以下信息进行编码
        // 4B  magic number（魔法数）
        // 1B version（版本）
        // 4B full length（消息长度）
        // 1B messageType（消息类型）
        // 1B codec（序列化类型）
        // 1B compress（压缩类型）
        // 4B  requestId（请求的Id）

        out.writeBytes(CsRpcConstants.MAGIC_NUMBER);
        out.writeByte(CsRpcConstants.VERSION);
        // 暂时不知数据长度 预留数据长度位置 数据流空出一个位置
        out.writerIndex(out.writerIndex() + 4);
        out.writeByte(csMessage.getMessageType());
        //序列化类型这里必须要先序列化后再压缩
        out.writeByte(csMessage.getCodec());
        out.writeByte(csMessage.getCompress());
        out.writeInt(ATOMIC_INTEGER.getAndIncrement());

        //对数据进行处理
        Object data = csMessage.getData();
        //header长度为16
        int fullLength = CsRpcConstants.HEAD_LENGTH;
        //先序列化
        Serializer serializer = loadSerializer(csMessage.getCodec());
        byte[] body = serializer.serialize(data);
        //再压缩数据
        Compress compress = loadCompress(csMessage.getCompress());
        body = compress.compress(body);
        //数据流总长度
        fullLength += body.length;
        //写入处理好的数据
        out.writeBytes(body);

        //将数据长度写到tcp协议上数据格式,将长度填到空的位置

        //writerIndex()函数 获得当前流最后一个bit的位置
        //writerIndex(index)函数 将写入流开始坐标移动到index下标位置

        //保存最后移位bit流的坐标
        int writerIndex = out.writerIndex();
        //将写入流的开始坐标,移动到留空位置
        out.writerIndex(writerIndex - fullLength + CsRpcConstants.MAGIC_NUMBER.length+1);
        //将fullLength写入流中
        out.writeInt(fullLength);
        //将坐标移动到流最后一位
        out.writerIndex(writerIndex);

    }
    private Serializer loadSerializer(byte codecType) {
        String serializerName = SerializationTypeEnum.getName(codecType);
        ServiceLoader<Serializer> load = ServiceLoader.load(Serializer.class);
        for (Serializer serializer : load) {
            if (serializer.name().equals(serializerName)) {
                return serializer;
            }
        }
        throw new CsRpcException("无对应的序列化类型");
    }

    private Compress loadCompress(byte compressType) {
        String compressName = CompressTypeEnum.getName(compressType);
        ServiceLoader<Compress> load = ServiceLoader.load(Compress.class);
        for (Compress compress : load) {
            if (compress.name().equals(compressName)) {
                return compress;
            }
        }
        throw new CsRpcException("无对应的压缩类型");
    }
}
