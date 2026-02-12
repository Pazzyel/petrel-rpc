package rpc.pazz.loadbalance.loadbalancer;

import com.google.common.hash.HashFunction;
import lombok.extern.slf4j.Slf4j;
import rpc.pazz.factory.SingletonFactory;
import rpc.pazz.loadbalance.AbstractLoadBalance;
import rpc.pazz.loadbalance.LoadBalance;
import rpc.pazz.remote.dto.RpcRequest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
 * 1. 单例模式创建对象，减少频繁创建对象带来的负载均衡消耗
 * 2. 每次重构服务器列表，采用了无锁（自旋锁） + 双锁检测，减少上下文切换的异常
 * 3. 重构服务器列表前，会对整个列表进行检测，减少无用的重构
 */
@Slf4j
public class ConsistentHashLoadBalance extends AbstractLoadBalance {

    private final ConcurrentHashMap<String, ConsistentHashingLoadBalancer> selectors = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<String> address, RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        ConsistentHashingLoadBalancer selector = selectors.get(rpcServiceName);

        if (selector == null) {
            //还没有自己的选择器就构造一个
            selector = SingletonFactory.getInstance(() -> new ConsistentHashingLoadBalancer(
                    address,160,new ConsistentHashingLoadBalancer.MD5HashFunction()
            ), ConsistentHashingLoadBalancer.class);
            selectors.put(rpcServiceName, selector);
        } else if (selector.hasChanged(address)) {
            //地址发生变化就重建hash环
            selector.rebuild(address);
        }
        //使用请求的uuid进行hash
        return selector.selectNode(rpcServiceName + rpcRequest.getRequestId());
    }

    static class ConsistentHashingLoadBalancer {
        /**
         * 哈希环定义部分：使用TreeMap存储虚拟节点的哈希值到物理节点的映射
         * 1. 虚拟结点
         * 2. hash函数
         * 3. TreeMap存储结点
         * 4. 物理结点列表
         * */
        //虚拟节点hash到物理节点的映射，找到第一个大于...，用TreeMap方便
        private final TreeMap<Long, String> virtualNodes = new TreeMap<>();
        //物理节点集合
        private final Set<String> physicalNodes = new HashSet<>();
        //每个物理节点配备几个虚拟节点
        private final int virtualNodeCount;
        //哈希函数
        private final HashFunction hashFunction;
        /**
         * 防止使用了没有初始化完成的选择器
         * */
        private volatile boolean initFlag = false;

        public interface HashFunction {
            long hash(String key);
        }

        public static class MD5HashFunction implements HashFunction {
            @Override
            public long hash(String key) {
                try {
                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    byte[] digest = md5.digest(key.getBytes());

                    // 取前8字节作为long类型的哈希值
                    return ((long) (digest[0] & 0xFF) << 56) |
                            ((long) (digest[1] & 0xFF) << 48) |
                            ((long) (digest[2] & 0xFF) << 40) |
                            ((long) (digest[3] & 0xFF) << 32) |
                            ((long) (digest[4] & 0xFF) << 24) |
                            ((long) (digest[5] & 0xFF) << 16) |
                            ((long) (digest[6] & 0xFF) << 8) |
                            (digest[7] & 0xFF);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public ConsistentHashingLoadBalancer(List<String> address, int virtualNodeCount, HashFunction hashFunction) {
            log.info("创建一致性哈希选择器");
            this.initFlag = false;
            this.virtualNodeCount = virtualNodeCount;
            this.hashFunction = hashFunction;
            //构建hash环
            for (String addr : address) {
                this.addNode(addr);
            }
            this.initFlag = true;
        }

        /**
         * 一致性哈希环算法选择节点
         */
        public String selectNode(String key) {
            //确保可用性
            while (!initFlag) {
                //自旋等待
            }
            //空节点检查
            if (this.virtualNodes.isEmpty()) {
                log.error("当前没有任何虚拟节点创建，选择失败");
                return null;
            }
            long hashKey = hashFunction.hash(key);
            Map.Entry<Long, String> entry = this.virtualNodes.ceilingEntry(hashKey);
            //没有更大的entry说明回到哈希环的开头
            if (entry == null) {
                entry = this.virtualNodes.firstEntry();
            }
            return entry.getValue();
        }

        /**
         * 添加新物理节点
         */
        private void addNode(String node) {
            if (physicalNodes.contains(node)) {
                return;
            }
            physicalNodes.add(node);
            //创建对应数量虚拟节点
            for (int i = 0; i < virtualNodeCount; i++) {
                String virtualNode = node + "#" + i;
                virtualNodes.put(hashFunction.hash(virtualNode), node);
            }
        }

        /**
         * 移除物理节点
         */
        private void removeNode(String node) {
            if (!physicalNodes.contains(node)) {
                return;
            }
            physicalNodes.remove(node);

            //移除该物理节点对应的所有虚拟节点
            for (int i = 0; i < virtualNodeCount; i++) {
                String virtualNodeName = node + "#" + i;
                virtualNodes.remove(hashFunction.hash(virtualNodeName));
            }
        }

        /**
         * 获取所有物理节点
         */
        public List<String> getAllNodes() {
            while (!initFlag) {
                //自旋等待，不要上下文切换
            }
            //不应当修改
            return Collections.unmodifiableList(new ArrayList<>(virtualNodes.values()));
        }

        /**
         * 判断地址列表有没有变化
         */
        public boolean hasChanged(List<String> address) {
            if (address.size() != this.physicalNodes.size()) {
                return true;
            }
            for (String node : address) {
                if (!this.physicalNodes.contains(node)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 根据新的地址列表重建哈希环
         */
        public synchronized void rebuild(List<String> address) {
            //修改标记表示当前状态不可用
            this.initFlag = false;
            if (!hasChanged(address)) {
                this.initFlag = true;
                return;
            }

            log.info("重构服务的选择器");
            List<String> removedNodes = new ArrayList<>();
            List<String> addedNodes = new ArrayList<>();
            Set<String> oldNodes = new HashSet<>(this.physicalNodes);
            Set<String> newNodes = new HashSet<>(address);
            for (String node : newNodes) {
                if (!oldNodes.contains(node)) {
                    addedNodes.add(node);
                }
            }
            for (String node : oldNodes) {
                if (!newNodes.contains(node)) {
                    removedNodes.add(node);
                }
            }

            //移除，新增对应节点
            for (String node : removedNodes) {
                this.removeNode(node);
            }
            for (String node : addedNodes) {
                this.addNode(node);
            }

            this.initFlag = true;
            log.info("重新构建的列表大小:{}", this.physicalNodes.size());
        }
    }
}
