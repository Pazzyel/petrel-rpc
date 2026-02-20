package rpc.petrel.async;

import rpc.petrel.exception.FutureResultGetException;
import rpc.petrel.exception.UnsupportedInvokeException;
import rpc.petrel.proxy.RpcClientProxy;
import rpc.petrel.remote.dto.RpcRequest;
import rpc.petrel.remote.dto.RpcResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RpcFuture<T> implements Future<T> {

    private final Future<RpcResponse<Object>> future;
    private final RpcRequest rpcRequest;

    public RpcFuture(Future<RpcResponse<Object>> future, RpcRequest rpcRequest) {
        this.future = future;
        this.rpcRequest = rpcRequest;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return this.future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.future.isDone();
    }

    //带有请求检查的Get
    @Override
    public T get() {
        try {
            RpcResponse<Object> rpcResponse = future.get();
            RpcClientProxy.check(rpcResponse, rpcRequest);
            return (T) rpcResponse.getData();
        } catch (ExecutionException | InterruptedException e) {
            throw new FutureResultGetException(e);
        }
    }

    //带有请求检查的Get
    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        RpcResponse<Object> rpcResponse = future.get(timeout, unit);
        RpcClientProxy.check(rpcResponse, rpcRequest);
        return (T) rpcResponse.getData();
    }

    public static void unsupported() throws UnsupportedInvokeException {
        throw new UnsupportedInvokeException("This method only should invoked in proxy class");
    }
}
