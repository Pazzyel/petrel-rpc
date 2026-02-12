package rpc.pazz.registry.impl.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import rpc.pazz.enums.LoadBalanceEnum;
import rpc.pazz.enums.RpcErrorMessageEnum;
import rpc.pazz.exception.RpcException;
import rpc.pazz.extension.ExtensionLoader;
import rpc.pazz.loadbalance.LoadBalance;
import rpc.pazz.registry.ServiceDiscovery;
import rpc.pazz.registry.utils.CuratorUtil;
import rpc.pazz.remote.dto.RpcRequest;
import rpc.pazz.utils.CollectionUtil;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
public class ZookeeperServiceDiscoveryImpl implements ServiceDiscovery {

    private final LoadBalance loadBalance;

    public ZookeeperServiceDiscoveryImpl() {
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(LoadBalanceEnum.LOADBALANCE.getName());
    }

    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        //获取服务名称
        String rpcServiceName = rpcRequest.getRpcServiceName();
        //获取Zookeeper客户端
        CuratorFramework zkClient = CuratorUtil.getZkClient();
        //获取服务的所有节点地址
        List<String> serviceUrlList = CuratorUtil.getChildrenNodes(zkClient, rpcServiceName);
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        //负载均衡算法选择一个服务主机地址
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);
        //拆分地址为主机+端口，返回InetSocketAddress
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        return new InetSocketAddress(host, port);
    }
}
