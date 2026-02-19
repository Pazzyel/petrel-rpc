package rpc.petrel.registry.utils;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import rpc.petrel.factory.SingletonFactory;
import rpc.petrel.properties.RpcProperties;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@NoArgsConstructor
public class CuratorUtil {

    private static final int BASE_SLEEP_TIME = 1000;
    private static final int MAX_RETRIES = 5;
    private static final int MAX_SLEEP_TIME = 30000;
    private static final int SESSION_TIMEOUT = 60000;
    private static final int CONNECTION_TIMEOUT = 15000;
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";

    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final Map<String, CuratorCache> SERVICE_WATCHER_MAP = new ConcurrentHashMap<>();
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();
    private static CuratorFramework zkClient;
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";

    private static final RpcProperties properties = SingletonFactory.getInstance(RpcProperties.class);

    public static void bindConnectionStateListener(CuratorFramework zkClient) {
        zkClient.getConnectionStateListenable().addListener((client, newState) -> {
            switch (newState) {
                case LOST -> {
                    // 会话丢失：缓存立即失效
                    SERVICE_ADDRESS_MAP.clear();
                    log.warn("ZK state LOST, local service cache invalidated");
                }
                case SUSPENDED -> log.warn("ZK state SUSPENDED");
                default -> {
                }
            }
        });
    }

    public static CuratorFramework getZkClient() {
        // check if user has set zk address
        // 获取rpc.properties的配置
        String zookeeperAddress = properties.getRegistryAddress();
        // if zkClient has been started, return directly
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }
        // Retry strategy. Retry 3 times, and will increase the sleep time between retries.
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES, MAX_SLEEP_TIME);
        zkClient = CuratorFrameworkFactory.builder()
                // the server to connect to (can be a server list)
                .connectString(zookeeperAddress)
                .sessionTimeoutMs(SESSION_TIMEOUT)
                .connectionTimeoutMs(CONNECTION_TIMEOUT)
                .retryPolicy(retryPolicy)
                .build();
        bindConnectionStateListener(zkClient);
        zkClient.start();
        try {
            // wait 30s until connect to the zookeeper
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Time out waiting to connect to ZK!");
            }
        } catch (InterruptedException e) {
            log.error("Exception while connecting to ZK!");
        }
        return zkClient;
    }

    /**
     * 创建Zookeeper永久节点
     * @param zkClient Curator客户端
     * @param path 节点路径
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path) {
        try {
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("The node already exists. The node is:[{}]", path);
            } else {
                //创建Zookeeper永久节点
                //eg: /my-rpc/helloService/127.0.0.1:9999 三部分 /my-rpc/{服务名称}/{服务地址}
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("The node was created successfully. The node is:[{}]", path);
            }
        } catch (Exception e) {
            log.error("Create persistent node for path [{}] fail", path);
        }
    }

    /**
     * 删除对应IP的节点
     * @param zkClient Curator客户端
     * @param inetSocketAddress 要删除的节点的IP
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        //并行的add必须线程安全
        final Set<String> removePathSet = ConcurrentHashMap.newKeySet();
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                    removePathSet.add(p);
                }
            } catch (Exception e) {
                log.error("clear registry for path [{}] fail", p);
            }
        });
        //清除已经移除的节点路径
        REGISTERED_PATH_SET.removeAll(removePathSet);
        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET);
    }
    /**
     * 创建Zookeeper临时节点
     * @param zkClient Curator客户端
     * @param path 节点路径
     * */
    public static void createEphemeralNode(CuratorFramework zkClient, String path) {
        try {
            zkClient.create().creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .withACL(Ids.OPEN_ACL_UNSAFE)//开放权限
                    .forPath(path, "status:ok".getBytes());
        } catch (Exception e) {
            log.error("创建临时结点失败!", e);
        }
    }
    /**
     * 删除临时节点
     */
    public static void deleteEphemeralNode(CuratorFramework zkClient, String path) {
        // 方法2：强制删除节点（无论是否有子节点）
        try {
            zkClient.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            log.error("删除临时结点：{}失败", path);
        }
    }

    /**
     * 获取一个节点下的所有子节点
     * @param rpcServiceName rpc service 名称，也是节点的路径
     * @return 路径下所有子节点
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {

        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }

        List<String> cached = SERVICE_ADDRESS_MAP.get(rpcServiceName);

        List<String> result = null;
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        try {
            result = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
            //不使用watcher
            //注册watcher监控子节点变化情况
            registerWatcher(zkClient, rpcServiceName);
        } catch (Exception e) {
            log.error("Get children nodes for path [{}] fail", servicePath);

            return cached;

        }
        return result;
    }

    /**
     * 为某个节点注册watcher检测子节点变化
     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version
     */
    private static void registerWatcher(CuratorFramework zkClient, String rpcServiceName) {
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        //缓存watcher防止重复注册
        SERVICE_WATCHER_MAP.computeIfAbsent(servicePath, key -> {
            CuratorCache cache = CuratorCache.build(zkClient, key);
            CuratorCacheListener listener = CuratorCacheListener.builder()//所有子节点更新内容都执行对应回调
                    .forCreates(childData -> refreshServiceAddresses(zkClient, rpcServiceName, key))
                    .forDeletes(childData -> refreshServiceAddresses(zkClient, rpcServiceName, key))
                    .forChanges((oldNode, childData) -> refreshServiceAddresses(zkClient, rpcServiceName, key))
                    .forInitialized(() -> refreshServiceAddresses(zkClient, rpcServiceName, key))
                    .build();
            cache.listenable().addListener(listener);
            cache.start();
            return cache;
        });
    }
    //修改map里服务名称对应的子节点内容
    private static void refreshServiceAddresses(CuratorFramework zkClient, String rpcServiceName, String servicePath) {
        try {
            List<String> serviceAddresses = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
        } catch (Exception e) {
            log.error("refresh service addresses for path [{}] fail", servicePath, e);
        }
    }
}
