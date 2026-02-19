package rpc.petrel.spring;

import org.springframework.context.annotation.Bean;
import rpc.petrel.bootstrap.RpcBootstrap;

public class RpcBeanConfiguration {

    @Bean
    public RpcServiceBeanPostProcessor rpcServiceBeanPostProcessor() {
        return new RpcServiceBeanPostProcessor();
    }

    @Bean
    public RpcReferenceBeanPostProcessor rpcReferenceBeanPostProcessor() {
        return new RpcReferenceBeanPostProcessor();
    }

    @Bean
    public RpcBootstrap rpcBootstrap() {
        return new RpcBootstrap();
    }
}
