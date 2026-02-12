package rpc.pazz.loadbalance;

import rpc.pazz.remote.dto.RpcRequest;
import rpc.pazz.utils.CollectionUtil;

import java.util.List;

public abstract class AbstractLoadBalance implements LoadBalance {
    @Override
    public String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest) {
        //先进行简单处理，没有地址时返回null
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            return null;
        }
        //只有一个地址返回这个地址
        if (serviceUrlList.size() == 1) {
            return serviceUrlList.get(0);
        }
        //多个地址调用负载均衡算法
        return doSelect(serviceUrlList, rpcRequest);
    }

    protected abstract String doSelect(List<String> serviceUrlList, RpcRequest rpcRequest);
}
