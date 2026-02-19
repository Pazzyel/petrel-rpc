package rpc.petrel.remote.transport.netty.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import rpc.petrel.enums.RpcResponseCodeEnum;
import rpc.petrel.factory.SingletonFactory;
import rpc.petrel.handler.RpcRequestHandler;
import rpc.petrel.properties.RpcProperties;
import rpc.petrel.remote.constants.RpcConstants;
import rpc.petrel.remote.dto.RpcMessage;
import rpc.petrel.remote.dto.RpcRequest;
import rpc.petrel.remote.dto.RpcResponse;

@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {

    private final RpcRequestHandler rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);

    private final RpcProperties properties = SingletonFactory.getInstance(RpcProperties.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof RpcMessage requestMessage) {
                log.info("server receive msg: [{}] ", msg);
                byte messageType = requestMessage.getMessageType();
                RpcMessage rpcMessage = RpcMessage.builder()
                        .codec(properties.getSerializationType().getCode())
                        .compress(properties.getCompressionType().getCode()).build();//RpcMessage发送是不构造requestId
                if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                    //是心跳请求
                    rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    rpcMessage.setData(RpcConstants.PONG);
                } else {
                    //是普通请求
                    RpcRequest rpcRequest = (RpcRequest) requestMessage.getData();
                    rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                    Object result = rpcRequestHandler.handle(rpcRequest);
                    log.info("server get result: : [{}] ", result);
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        //使用request的id构造对应的response
                        RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                        rpcMessage.setData(rpcResponse);
                    } else {
                        log.error("server write fail: [{}] ", rpcRequest);
                        RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                        rpcMessage.setData(rpcResponse);
                    }
                }
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleStateEvent) {
            IdleState state = idleStateEvent.state();
            if (IdleState.READER_IDLE.equals(state)) {
                //30s空闲计时器触发，关闭连接
                log.info("Idle check happen, close the connection: [{}]", state);
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Server exception caught: [{}]", cause.getMessage(), cause);
        ctx.close();
    }
}
