package rpc.petrel.spring;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import rpc.petrel.annotation.RpcReference;
import rpc.petrel.config.RpcServiceConfig;
import rpc.petrel.extension.ExtensionLoader;
import rpc.petrel.factory.SingletonFactory;
import rpc.petrel.properties.RpcProperties;
import rpc.petrel.proxy.RpcClientProxy;
import rpc.petrel.remote.transport.RpcRequestTransport;

import java.lang.reflect.Field;

//该Bean会加载RpcClient
@Slf4j
@Component
public class RpcReferenceBeanPostProcessor implements BeanPostProcessor {

    private final RpcRequestTransport rpcClient;

    private final RpcProperties properties;

    public RpcReferenceBeanPostProcessor() {
        this.properties = SingletonFactory.getInstance(RpcProperties.class);
        this.rpcClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class).getExtension(properties.getClientType().getName());
    }

    @Override
    public @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        //获取字段
        Field[] declaredFields = beanClass.getDeclaredFields();
        //扫描字段山的RpcReference注解
        for (Field declaredField : declaredFields) {
            if (declaredField.isAnnotationPresent(RpcReference.class)) {
                RpcReference rpcReference = declaredField.getAnnotation(RpcReference.class);
                //注意作为服务需求方的config不需要包含服务对象
                RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                        .group(rpcReference.group())
                        .version(rpcReference.version()).build();
                RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient, rpcServiceConfig);
                Object proxy = rpcClientProxy.getProxy(declaredField.getType());
                declaredField.setAccessible(true);
                try {
                    declaredField.set(bean, proxy);
                } catch (IllegalAccessException e) {
                    log.error("Requirement inject failed", e);
                }
            }
        }
        return bean;
    }
}
