package rpc.petrel.spring;

import org.springframework.context.annotation.Bean;
import rpc.petrel.bootstrap.RpcBootstrap;
import rpc.petrel.filter.FilterRegistrarBeanFactoryPostProcessor;
import rpc.petrel.serialize.kryo.KryoRegistrarBeanFactoryPostProcessor;

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

    @Bean
    public KryoRegistrarBeanFactoryPostProcessor kryoRegistrarBeanFactoryPostProcessor() {
        return new KryoRegistrarBeanFactoryPostProcessor();
    }

    @Bean
    public FilterRegistrarBeanFactoryPostProcessor filterRegistrarBeanFactoryPostProcessor() {
        return new FilterRegistrarBeanFactoryPostProcessor();
    }
}
