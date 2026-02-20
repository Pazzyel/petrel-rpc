package rpc.petrel.test.api;

import rpc.petrel.annotation.AsyncMethod;

import java.util.concurrent.CompletableFuture;

public interface HelloService {
    Person sayHello(Name name, Long age);

    @AsyncMethod
    default CompletableFuture<Person> sayHelloAsync(Name name, Long age) {
        return CompletableFuture.completedFuture(sayHello(name, age));
    }
}
