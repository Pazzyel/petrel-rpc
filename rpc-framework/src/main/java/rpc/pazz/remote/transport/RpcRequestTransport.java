package rpc.pazz.remote.transport;

import rpc.pazz.remote.dto.RpcRequest;

public interface RpcRequestTransport {
    /**
     * send rpc request to server and get result
     * @param rpcRequest message body
     * @return data from server
     */
    Object sendRpcRequest(RpcRequest rpcRequest);
}
