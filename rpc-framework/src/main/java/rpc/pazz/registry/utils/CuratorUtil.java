package rpc.pazz.registry.utils;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import rpc.pazz.enums.RpcConfigEnum;
import rpc.pazz.utils.PropertiesFileUtil;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@NoArgsConstructor
public class CuratorUtil {

    private static final int BASE_SLEEP_TIME = 1000;
    private static final int MAX_RETRIES = 3;
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();
    private static CuratorFramework zkClient;
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";

    public static CuratorFramework getZkClient() {
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        String zookeeperAddress = properties != null &&
                properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null
                ? properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) : DEFAULT_ZOOKEEPER_ADDRESS;
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        zkClient = CuratorFrameworkFactory.builder()
                .connectString(zookeeperAddress)
                .retryPolicy(retryPolicy)
                .build();
        zkClient.start();
        try {
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Time out waiting to connect to ZK!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Exception while connecting to ZK!", e);
        }
        return zkClient;
    }

    public static void createPersistentNode(CuratorFramework zkClient, String path) {
        try {
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("The node already exists. The node is:[{}]", path);
            } else {
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("The node was created successfully. The node is:[{}]", path);
            }
        } catch (Exception e) {
            log.error("Create persistent node for path [{}] fail", path, e);
        }
    }

    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        final Set<String> removePathSet = ConcurrentHashMap.newKeySet();
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                    removePathSet.add(p);
                }
            } catch (Exception e) {
                log.error("clear registry for path [{}] fail", p, e);
            }
        });
        REGISTERED_PATH_SET.removeAll(removePathSet);
        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET);
    }

    public static void createEphemeralNode(CuratorFramework zkClient, String path) {
        try {
            zkClient.create().creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .withACL(Ids.OPEN_ACL_UNSAFE)
                    .forPath(path, "status:ok".getBytes());
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("Create ephemeral node failed for path [{}]", path, e);
        }
    }

    public static void deleteEphemeralNode(CuratorFramework zkClient, String path) {
        try {
            zkClient.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            log.error("Delete ephemeral node failed for path [{}]", path, e);
        }
    }

    /**
     * Client-side service discovery:
     * use foreground query each time and update cache.
     * This avoids CuratorCache background watcher retries spamming logs when ZK connection jitters.
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        List<String> cached = SERVICE_ADDRESS_MAP.get(rpcServiceName);
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        try {
            List<String> latest = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, latest);
            return latest;
        } catch (Exception e) {
            log.error("Get children nodes for path [{}] fail", servicePath, e);
            return cached;
        }
    }
}
