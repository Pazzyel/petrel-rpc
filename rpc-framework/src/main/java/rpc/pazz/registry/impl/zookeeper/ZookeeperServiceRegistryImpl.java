package rpc.pazz.registry.impl.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import rpc.pazz.registry.ServiceRegistry;
import rpc.pazz.registry.utils.CuratorUtil;

import java.net.InetSocketAddress;

public class ZookeeperServiceRegistryImpl implements ServiceRegistry {
    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        //获取完整路径
        String servicePath = CuratorUtil.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorFramework zkClient = CuratorUtil.getZkClient();
        //CuratorUtil.createPersistentNode(zkClient, servicePath);
        //考虑到主机可能和Zookeeper断开，应该创建临时节点保证Zookeeper可以及时清除无效节点
        CuratorUtil.createEphemeralNode(zkClient, servicePath);
    }
}
