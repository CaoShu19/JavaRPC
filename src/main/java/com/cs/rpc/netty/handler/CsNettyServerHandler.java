package com.cs.rpc.netty.handler;

import com.cs.rpc.constant.CsRpcConstants;
import com.cs.rpc.constant.MessageTypeEnum;
import com.cs.rpc.factory.SingletonFactory;
import com.cs.rpc.message.CsMessage;
import com.cs.rpc.message.CsRequest;
import com.cs.rpc.message.CsResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ：cs
 * @description：消息处理类,对线程池的处理,继承通道处理适配器
 * @date ：2022/11/19 0:14
 */

@Slf4j
public class CsNettyServerHandler extends ChannelInboundHandlerAdapter {
    private CsRequestHandler csRequestHandler;

    public CsNettyServerHandler(){
        csRequestHandler = SingletonFactory.getInstance(CsRequestHandler.class);
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //触发心跳检测后执行此函数
        if(evt instanceof IdleStateEvent){
            //强转一下使用
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            IdleState state = stateEvent.state();
            if(state == IdleState.READER_IDLE){
                //事件的状态是 未读状态
                log.info("收到心跳检测,超时未读取....");

            }else {
                super.userEventTriggered(ctx,evt);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //出现异常,那么关闭服务
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //此方法的作用是接收客服端发来的数据(解码后),我们进行可以重写后对数据进行处理
        //由于是RPC服务,客服端传来的数据包括要调用服务提供者的接口和方法
        //重写后,我们解析消息,找到对应的服务提供者,然后调用服务,得到掉用结果,返回给客服端(编码后)
        try {
            if(msg instanceof CsMessage){
                //拿到数据信息对象,调用对应服务提供方法,获取结果,然后返回给客服端''
                CsMessage csMessage = (CsMessage) msg;
                byte messageType = csMessage.getMessageType();

                if(messageType == MessageTypeEnum.HEARTBEAT_PING.getCode()){
                    //返回心跳包
                    csMessage.setData(CsRpcConstants.PONG);
                    csMessage.setMessageType(MessageTypeEnum.HEARTBEAT_PONG.getCode());
                }
                if(messageType == MessageTypeEnum.REQUEST.getCode()) {
                    //如果是请求,那么就处理请求业务
                    CsRequest csRequest = (CsRequest) csMessage.getData();
                    //处理业务,使用反射找到方法 发起调用 获取代理类执行后的结果
                    Object result = csRequestHandler.handler(csRequest);
                    //给csMessage设置成response类型
                    csMessage.setMessageType(MessageTypeEnum.RESPONSE.getCode());
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        //如果通道保活 或 通道可写 将数据返回
                        //创建返回response,将数据结果放入Message
                        CsResponse csResponse = CsResponse.success(result, csRequest.getRequestId());
                        //将response放入要返回的信息对象
                        csMessage.setData(csResponse);
                        log.info("返回调用服务后得到的结果:",csMessage);
                    } else {
                        csMessage.setData(CsResponse.fail("net fail,channel error"));
                    }
                    //将信息体写回到channel中,并添加一个监听器,保证数据要全部写完到通道中
                    ctx.writeAndFlush(csMessage).addListener(ChannelFutureListener.CLOSE);
                }
            }
        }catch (Exception e){
            log.error("读取消息出错:",e);
        }finally {
            //释放内存,以防内存泄漏
            ReferenceCountUtil.release(msg);
        }
    }
}
