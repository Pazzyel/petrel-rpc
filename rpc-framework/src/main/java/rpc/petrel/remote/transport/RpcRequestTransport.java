package rpc.petrel.remote.transport;

import rpc.petrel.extension.SPI;
import rpc.petrel.remote.dto.RpcRequest;

@SPI
public interface RpcRequestTransport {
    /**
     * send rpc request to server and get result
     * @param rpcRequest message body
     * @return data from server
     */
    Object sendRpcRequest(RpcRequest rpcRequest);
}
