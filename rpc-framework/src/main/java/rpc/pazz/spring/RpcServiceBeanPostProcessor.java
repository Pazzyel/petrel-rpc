package rpc.pazz.spring;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import rpc.pazz.annotation.RpcService;
import rpc.pazz.config.RpcServiceConfig;
import rpc.pazz.factory.SingletonFactory;
import rpc.pazz.provider.ServiceProvider;
import rpc.pazz.provider.impl.ZookeeperServiceProviderImpl;

@Slf4j
@Component
public class RpcServiceBeanPostProcessor implements BeanPostProcessor {

    private final ServiceProvider serviceProvider;

    public RpcServiceBeanPostProcessor() {
        this.serviceProvider = SingletonFactory.getInstance(ZookeeperServiceProviderImpl.class);
    }

    @Override
    public @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(RpcService.class)) {
            log.info("[{}] is annotated with  [{}]", bean.getClass().getName(), RpcService.class.getName());
            RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
            RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                    .group(rpcService.group())
                    .version(rpcService.version())
                    .service(bean).build();//主动注册服务的地方RpcServiceConfig需要实例
            //保存到程序的provider中并发布服务
            serviceProvider.publishService(rpcServiceConfig);
        }
        return bean;
    }
}
