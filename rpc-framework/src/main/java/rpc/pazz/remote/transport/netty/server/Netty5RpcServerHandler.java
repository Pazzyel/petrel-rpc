package rpc.pazz.remote.transport.netty.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutorGroup;
import lombok.extern.slf4j.Slf4j;
import rpc.pazz.enums.CompressTypeEnum;
import rpc.pazz.enums.RpcResponseCodeEnum;
import rpc.pazz.enums.SerializationTypeEnum;
import rpc.pazz.factory.SingletonFactory;
import rpc.pazz.handler.RpcRequestHandler;
import rpc.pazz.properties.RpcProperties;
import rpc.pazz.remote.constants.RpcConstants;
import rpc.pazz.remote.dto.RpcMessage;
import rpc.pazz.remote.dto.RpcRequest;
import rpc.pazz.remote.dto.RpcResponse;

import java.util.concurrent.ExecutorService;

@Slf4j
public class Netty5RpcServerHandler extends ChannelInboundHandlerAdapter {

    private final RpcRequestHandler rpcRequestHandler;
    private final ExecutorService executor;

    private final RpcProperties properties;

    public Netty5RpcServerHandler(EventExecutorGroup executor) {
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
        this.executor = executor;

        this.properties = SingletonFactory.getInstance(RpcProperties.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof RpcMessage)) {
            ReferenceCountUtil.release(msg);
            return;
        }
        ReferenceCountUtil.retain(msg); //引用计数+1防止进入其它线程后对象被GC
        //使用独立线程池执行业务，不阻塞netty的IO
        this.executor.submit(() -> {
            try {
                RpcMessage requestMessage = (RpcMessage) msg;
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
                    log.info("server get result: [{}] ", result);
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
            } finally {
                ReferenceCountUtil.release(msg);
            }
        });
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
