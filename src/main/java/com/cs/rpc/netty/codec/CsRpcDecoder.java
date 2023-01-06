package com.cs.rpc.netty.codec;

import com.cs.rpc.compress.Compress;
import com.cs.rpc.constant.CompressTypeEnum;
import com.cs.rpc.constant.CsRpcConstants;
import com.cs.rpc.constant.MessageTypeEnum;
import com.cs.rpc.constant.SerializationTypeEnum;
import com.cs.rpc.exception.CsRpcException;
import com.cs.rpc.message.CsMessage;
import com.cs.rpc.message.CsRequest;
import com.cs.rpc.message.CsResponse;
import com.cs.rpc.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.util.ServiceLoader;

/**
 * @author ：cs
 * @description：
 * TCP粘包拆包问题，在于无法准确的标识接收到的数据，
 * 我们使用LengthFieldBasedFrameDecoder和自定义协议的形式来标识
 * 我们需要的数据，就可以进行准确的辩识，从而解决粘包拆包问题。
 *
 * 自定义读取报文的长度,来实现解决粘包和拆包问题
 *
 * @date ：2022/11/19 2:49
 */

/**
 *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
 *   |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
 *   +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 *   |                                                                                                       |
 *   |                                         body                                                          |
 *   |                                                                                                       |
 *   |                                        ... ...                                                        |
 *   +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 */
public class CsRpcDecoder extends LengthFieldBasedFrameDecoder {

    public CsRpcDecoder(){
        //无参构造decoder 给出默认值
        this(8*1024*1024,5,4,-9,0);
    }

    /**
     *
     * @param maxFrameLength 最大帧长度。它决定可以接收的数据的最大长度。如果超过，数据将被丢弃,根据实际环境定义
     * @param lengthFieldOffset 数据长度字段开始的偏移量, magic code+version=长度为5,5个字节后才是长度域
     * @param lengthFieldLength 消息长度的大小  full length（消息长度） 长度为4
     * @param lengthAdjustment 补偿值 lengthAdjustment+数据长度取值=长度字段之后剩下包的字节数(x + 16=7 so x = -9)
     * @param initialBytesToStrip 忽略的字节长度，如果要接收所有的header+body 则为0，如果只接收body 则为header的长度 ,我们这为0
     */
    public CsRpcDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        //自定义构造 TCP数据报的解码模式
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {


        //继承原有类得到数据
        Object decode = super.decode(ctx, in);

        //对数据进行解码
        if(decode instanceof ByteBuf){
            //将数据读取到数据缓冲区 将其称为帧
            ByteBuf frame = (ByteBuf)decode;
            //对帧进行解码

            //获得可读的数据长度
            int length = frame.readableBytes();
            //对数据帧进行判断,若可读长度小于自定义的TCP报长度,那么就说明数据帧长度不够
            // 不满足自己定义的读取长度要求,抛出异常
            if(length < CsRpcConstants.TOTAL_LENGTH){
                throw new CsRpcException("帧数据长度过低");
            }
            //返回自己解码后的数据
            return decodeFrame(frame);
        }
        return decode;
    }

    /**
     * 对数据进行解码,TCP协议自定义的关键部分
     * @param frame
     * @return
     */
    private Object decodeFrame(ByteBuf frame) {

        //1.魔法数:本质就是一个标记,标记是自己rpc传来的数据
        //      检测是否是按照我们定义TCP协议传来的数据
        checkMagicCode(frame);
        //2.检测版本
        checkVersion(frame);
        //3.数据长度读取
        int fullLength = frame.readInt();
        //4.messageType 消息类型获取
        byte messageType = frame.readByte();
        //5.序列化类型读取
        byte codec = frame.readByte();
        //6.压缩类型读取
        byte compressType = frame.readByte();
        //7.请求id读取
        int requestId = frame.readInt();
        //8.构建数据流对应的信息对象,方便jvm使用
        CsMessage csMessage = CsMessage.builder()
                .messageType(messageType)
                .codec(codec)
                .compress(compressType)
                .requestId(requestId)
                .build();

        //9.数据体解码
        //获取数据长度
        int dataLength =fullLength -  CsRpcConstants.TOTAL_LENGTH;
        if(dataLength > 0){
            //有数据,就去数据提body读取数据
            byte[] data = new byte[dataLength];
            //1.将帧中数据体读到data中
            frame.readBytes(data);
            //2.根据压缩类型进行解压缩(用最为广泛的gzip)
            //获得具体的压缩类对象(里面实现可以压缩数据的压缩类)
            Compress compress = loadCompress(compressType);
            data = compress.decompress(data);
            //3.根据序列化类型反序列化
            //用protostuff进行序列化和反序列化
            //获得具体序列化类对象(序列化:数据流 <-- object 反序列化:数据流-->object)
            Serializer serializer = loadSerializer(codec);
            //这里要根据不同的业务进行反序列化
            //  消费端发送请求   服务端响应数据
            //  那么就有两种反序列化 将数据流反序列成 request 或者 response
            //  所以我们要自定义两种类 CsRequest CsResponse
            //如果信息类型是请求
            if(MessageTypeEnum.REQUEST.getCode() == messageType){
                CsRequest csRequest = (CsRequest)serializer.deserialize(data, CsRequest.class);
                csMessage.setData(csRequest);
            }
            if(MessageTypeEnum.RESPONSE.getCode() == messageType){
                CsResponse csResponse = (CsResponse)serializer.deserialize(data, CsResponse.class);
                csMessage.setData(csResponse);
            }

        }

        return csMessage;

    }

    private Serializer loadSerializer(byte codec) {
        //通过SPI实现服务发现
        //  得到序列化类型的字符串名字
        String name = SerializationTypeEnum.getName(codec);

        ServiceLoader<Serializer> load = ServiceLoader.load(Serializer.class);
        for (Serializer serializer : load) {
            if(serializer.name().equals(name)){
                return serializer;
            }
        }
        throw new CsRpcException("传来的数据解析中,无对应的序列化类型");
    }

    private Compress loadCompress(byte compressType) {
        //通过SPI实现服务发现
        //  得到压缩类型的字符串名字
        String name = CompressTypeEnum.getName(compressType);

        ServiceLoader<Compress> load = ServiceLoader.load(Compress.class);
        for (Compress compress : load) {
            if(name.equals(compress.name())){
                return compress;
            }
        }
        return null;
    }

    private void checkVersion(ByteBuf frame) {
        byte b = frame.readByte();
        if(b != CsRpcConstants.VERSION){
            throw new CsRpcException("版本对不上,可能失效version");
        }
    }

    private void checkMagicCode(ByteBuf frame) {
        int length = CsRpcConstants.MAGIC_NUMBER.length;
        byte[] tmp = new byte[length];
        //将帧数据读到tmp中
        frame.readBytes(tmp);
        for (int i = 0; i < length; i++) {
            if(tmp[i] != CsRpcConstants.MAGIC_NUMBER[i]){
                throw new CsRpcException("魔法数对不上,不是自己定义协议的消息");
            }
        }
    }

}
