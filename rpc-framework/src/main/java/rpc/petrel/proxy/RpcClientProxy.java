package rpc.petrel.proxy;

import lombok.extern.slf4j.Slf4j;
import rpc.petrel.annotation.AsyncMethod;
import rpc.petrel.async.RpcFuture;
import rpc.petrel.config.RpcServiceConfig;
import rpc.petrel.enums.RpcErrorMessageEnum;
import rpc.petrel.enums.RpcResponseCodeEnum;
import rpc.petrel.exception.IllegalMethodNameException;
import rpc.petrel.exception.RpcException;
import rpc.petrel.remote.dto.RpcRequest;
import rpc.petrel.remote.dto.RpcResponse;
import rpc.petrel.remote.transport.RpcRequestTransport;
import rpc.petrel.utils.StringUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.Future;

@Slf4j
public class RpcClientProxy implements InvocationHandler {

    private static final String INTERFACE_NAME = "interfaceName";
    private final RpcRequestTransport rpcRequestTransport;
    private final RpcServiceConfig rpcServiceConfig;
    private static final String ASYNC_METHOD_SUFFIX = "Async";

    public RpcClientProxy(RpcRequestTransport rpcRequestTransport, RpcServiceConfig rpcServiceConfig) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = rpcServiceConfig;
    }

    /**
     * 获取代理对象
     * @param clazz 代理实现的接口
     * @return 代理对象
     * @param <T> 接口类型
     */
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //创建一个RPC请求
        log.info("invoked method: [{}]", method.getName());
        AsyncMethod asyncMethod = method.getAnnotation(AsyncMethod.class);
        if (asyncMethod != null) {
            String originMethodName = asyncMethod.originMethod();
            if (StringUtil.isBlank(originMethodName)) {
                String methodName = method.getName();
                if (methodName.endsWith(ASYNC_METHOD_SUFFIX)) {
                    originMethodName = methodName.substring(0, methodName.length() - ASYNC_METHOD_SUFFIX.length());
                } else {
                    throw new IllegalMethodNameException("Async method name must end \"Async\" or designated origin method name in @AsyncMethod annotation");
                }
            }
            RpcRequest rpcRequest = RpcRequest.builder()
                    .methodName(originMethodName)//替换成原始方法名称
                    .interfaceName(method.getDeclaringClass().getName())//要和RpcConfig的一致
                    .paramTypes(method.getParameterTypes())
                    .parameters(args)
                    .requestId(UUID.randomUUID().toString()) //和RpcMessage自增的requestId，用于标识消息不一样，这个是随机的，用于标识请求/响应
                    .group(rpcServiceConfig.getGroup())//要和RpcConfig的一致
                    .version(rpcServiceConfig.getVersion())
                    .build();
            Future<RpcResponse<Object>> future = rpcRequestTransport.sendRpcRequestAsync(rpcRequest);
            // 返回自定义future类型
            return new RpcFuture<>(future,rpcRequest);
        } else {
            RpcRequest rpcRequest = RpcRequest.builder()
                    .methodName(method.getName())
                    .interfaceName(method.getDeclaringClass().getName())//要和RpcConfig的一致
                    .paramTypes(method.getParameterTypes())
                    .parameters(args)
                    .requestId(UUID.randomUUID().toString()) //和RpcMessage自增的requestId，用于标识消息不一样，这个是随机的，用于标识请求/响应
                    .group(rpcServiceConfig.getGroup())//要和RpcConfig的一致
                    .version(rpcServiceConfig.getVersion())
                    .build();
            RpcResponse<Object> rpcResponse = (RpcResponse<Object>) rpcRequestTransport.sendRpcRequest(rpcRequest);
            check(rpcResponse, rpcRequest);
            return rpcResponse.getData();
        }
    }

    //检查返回的响应是否合法
    public static void check(RpcResponse<Object> rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
        //是不是这个请求的响应
        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
        //请求是否成功
        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
    }
}
