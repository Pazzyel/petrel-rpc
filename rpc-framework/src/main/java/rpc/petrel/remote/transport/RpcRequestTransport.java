package rpc.petrel.remote.transport;

import rpc.petrel.extension.SPI;
import rpc.petrel.remote.dto.RpcRequest;
import rpc.petrel.remote.dto.RpcResponse;

import java.util.concurrent.Future;

@SPI
public interface RpcRequestTransport {
    /**
     * send rpc request to server and get result, not include heartbeat
     * @param rpcRequest message body
     * @return data from server
     */
    Object sendRpcRequest(RpcRequest rpcRequest);

    /**
     * send rpc request to server and get async result
     * @param rpcRequest message body
     * @return CompletableFuture object
     * 为什么不返回Future而是CompletableFuture，详见async下的RpcFuture
     */
    Future<RpcResponse<Object>> sendRpcRequestAsync(RpcRequest rpcRequest);
}
