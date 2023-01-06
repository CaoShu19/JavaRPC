package com.cs.rpc.netty.client.handler;

import com.cs.rpc.compress.Compress;
import com.cs.rpc.constant.CompressTypeEnum;
import com.cs.rpc.constant.CsRpcConstants;
import com.cs.rpc.constant.MessageTypeEnum;
import com.cs.rpc.constant.SerializationTypeEnum;
import com.cs.rpc.factory.SingletonFactory;
import com.cs.rpc.message.CsMessage;
import com.cs.rpc.message.CsResponse;
import com.cs.rpc.netty.handler.UnprocessedRequests;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ：cs
 * @description："netty客服端处理"
 * @date ：2022/11/21 17:36
 */
@Slf4j
public class CsNettyClientHandler extends ChannelInboundHandlerAdapter {

    private UnprocessedRequests unprocessedRequests;
    public CsNettyClientHandler(){
        unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try{
            if(msg instanceof CsMessage){
                //读取数据 如果是response的消息类型，拿到数据，标识为完成
                CsMessage csMessage = (CsMessage)msg;

                if(MessageTypeEnum.RESPONSE.getCode() == csMessage.getMessageType()){
                    Object data = csMessage.getData();
                    CsResponse csResponse = (CsResponse) data;
                    unprocessedRequests.complete(csResponse);
                }

            }
        }catch (Exception e){
            log.error("客服端读取消息出错:",e);
        }finally {
            //释放内存,以防内存泄漏
            ReferenceCountUtil.release(msg);
        }

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //心跳检测测试代码
        if(evt instanceof IdleStateEvent){
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            if(stateEvent.state() == IdleState.WRITER_IDLE){
                log.info("客户端发送了心跳包");
                //3s一次心跳检测 ,将evt的状态设置成write_idle
                //满足心跳检测的机会,发送心跳包请求去服务器
                CsMessage csMessage = CsMessage.builder()
                        .messageType(MessageTypeEnum.HEARTBEAT_PING.getCode())
                        .compress(CompressTypeEnum.GZIP.getCode())
                        .codec(SerializationTypeEnum.PROTO_STUFF.getCode())
                        .data(CsRpcConstants.PING)
                        .build();
                //发送
                ctx.channel().writeAndFlush(csMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);;
            }
        }else {
            super.userEventTriggered(ctx,evt);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        log.info("客户端链接...正常");
        //表示连接的正常状体
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //触发此方法后 代表服务端关闭链接了
        super.channelInactive(ctx);
        log.info("服务端关闭了连接");
        //清除缓存

        //表示连接的关闭
        ctx.fireChannelInactive();
    }
}
