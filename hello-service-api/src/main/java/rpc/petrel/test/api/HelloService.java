package rpc.petrel.test.api;

import rpc.petrel.annotation.AsyncMethod;
import rpc.petrel.async.RpcFuture;

public interface HelloService {
    Person sayHello(Name name, Long age);

    @AsyncMethod
    default RpcFuture<Person> sayHelloAsync(Name name, Long age) {
        RpcFuture.unsupported();
        return null;
    }
}
