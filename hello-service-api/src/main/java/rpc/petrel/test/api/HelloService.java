package rpc.petrel.test.api;

import rpc.petrel.annotation.AsyncMethod;
import rpc.petrel.async.RpcFuture;

public interface HelloService {
    Person sayHello(String name);

    @AsyncMethod(originMethod = "sayHello")
    default RpcFuture<Person> sayHelloAlso(String name) {
        RpcFuture.unsupported();
        return null;
    }
}
