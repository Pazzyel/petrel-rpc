package rpc.petrel.registry;

import rpc.petrel.extension.SPI;
import rpc.petrel.remote.dto.RpcRequest;

import java.net.InetSocketAddress;

@SPI
public interface ServiceDiscovery {
    /**
     * return the net address of service
     * @param request RPC request object
     * @return the net address of service
     */
    InetSocketAddress lookupService(RpcRequest request);
}
