package rpc.pazz.handler;

import lombok.extern.slf4j.Slf4j;
import rpc.pazz.factory.SingletonFactory;
import rpc.pazz.provider.ServiceProvider;
import rpc.pazz.remote.dto.RpcRequest;
import rpc.pazz.remote.dto.RpcResponse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class RpcRequestHandler {
    private final ServiceProvider serviceProvider;

    public RpcRequestHandler() {
        this.serviceProvider = SingletonFactory.getInstance(ServiceProvider.class);
    }

    //Server调用以处理业务
    public Object handle(RpcRequest rpcRequest) {
        //从服务储存类中找到对应服务实例
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        return invokeTargetMethod(rpcRequest, service);
    }

    /**
     * 在Service实例上执行RPC请求目标方法
     * @param rpcRequest 请求内容
     * @param service 服务实例
     * @return 处理结果
     */
    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        try {
            log.info("service:[{}] successful invoke method:[{}]", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
            Method method = service.getClass().getDeclaredMethod(rpcRequest.getMethodName(),rpcRequest.getParamTypes());
            return method.invoke(service,rpcRequest.getParameters());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
