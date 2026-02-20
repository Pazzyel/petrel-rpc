package rpc.petrel.test.api;

import rpc.petrel.annotation.AsyncMethod;
import rpc.petrel.async.RpcFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public interface HelloService {
    Person sayHello(Name name, Long age);

    @AsyncMethod
    default Future<Person> sayHelloAsync(Name name, Long age) {
        return CompletableFuture.completedFuture(sayHello(name, age));
    }
}
