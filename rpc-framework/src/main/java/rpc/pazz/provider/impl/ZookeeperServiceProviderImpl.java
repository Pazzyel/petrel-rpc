package rpc.pazz.provider.impl;

import lombok.extern.slf4j.Slf4j;
import rpc.pazz.config.RpcServiceConfig;
import rpc.pazz.enums.RpcErrorMessageEnum;
import rpc.pazz.enums.ServiceDiscoveryEnum;
import rpc.pazz.enums.ServiceRegistryEnum;
import rpc.pazz.exception.RpcException;
import rpc.pazz.extension.ExtensionLoader;
import rpc.pazz.provider.ServiceProvider;
import rpc.pazz.registry.ServiceRegistry;
import rpc.pazz.registry.impl.zookeeper.ZookeeperServiceRegistryImpl;
import rpc.pazz.remote.transport.netty.server.NettyRpcServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存程序内所有以@RpcService标明的服务实例
 */
@Slf4j
public class ZookeeperServiceProviderImpl implements ServiceProvider {

    /**
     * key: rpc service name(interface name + version + group)
     * value: service object
     */
    private final Map<String, Object> serviceMap;//服务实例map
    private final Set<String> registeredService;//已经注册的服务名称
    private final ServiceRegistry serviceRegistry;//服务注册器

    public ZookeeperServiceProviderImpl() {
        this.serviceMap = new ConcurrentHashMap<>();
        this.registeredService = ConcurrentHashMap.newKeySet();
        this.serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension(ServiceRegistryEnum.ZK.getName());
    }

    /**
     * 添加服务，只是吧服务添加到Provider，并没有发布到注册中心
     * @param rpcServiceConfig 服务的配置
     */
    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        //重复注册就直接返回
        if (registeredService.contains(rpcServiceName)) {
            return;
        }
        registeredService.add(rpcServiceName);
        serviceMap.put(rpcServiceName, rpcServiceConfig.getService());
        log.info("Add service: {} and interfaces:{}", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }

    /**
     * 返回Provider存储的服务
     * @param rpcServiceName rpc service name(interface name + version + group)
     * @return 服务对象
     */
    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    /**
     * 发布服务，用自己的主机地址作为节点内容向Zookeeper发布服务
     * @param rpcServiceConfig rpc service related attributes
     */
    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            this.addService(rpcServiceConfig);
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, NettyRpcServer.PORT));
        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }
    }
}
