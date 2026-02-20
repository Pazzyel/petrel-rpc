package rpc.petrel.test.api;

import rpc.petrel.annotation.AsyncMethod;
import rpc.petrel.async.RpcFuture;
import rpc.petrel.exception.UnsupportedInvokeException;

public interface HelloService {
    String sayHello(String name);

    @AsyncMethod(originMethod = "sayHello")
    default RpcFuture<String> sayHelloAlso(String name) {
        RpcFuture.unsupported();
        return null;
    }
}
