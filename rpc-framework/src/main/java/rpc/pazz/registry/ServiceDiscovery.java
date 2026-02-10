package rpc.pazz.registry;

import rpc.pazz.remote.dto.RpcRequest;

import java.net.InetSocketAddress;

public interface ServiceDiscovery {
    /**
     * return the net address of service
     * @param request RPC request object
     * @return the net address of service
     */
    InetSocketAddress lookupService(RpcRequest request);
}
