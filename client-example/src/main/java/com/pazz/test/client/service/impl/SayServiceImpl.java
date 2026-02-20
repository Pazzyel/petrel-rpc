package com.pazz.test.client.service.impl;

import com.pazz.test.client.service.SayService;
import org.springframework.stereotype.Service;
import rpc.petrel.annotation.RpcReference;
import rpc.petrel.test.api.HelloService;
import rpc.petrel.test.api.Name;
import rpc.petrel.test.api.Person;

import java.util.concurrent.CompletableFuture;

@Service
public class SayServiceImpl implements SayService {

    @RpcReference(group = "test1", version = "version1")
    private HelloService helloService;

    @Override
    public void say(int count) {
        String name = "Pazz";
        CompletableFuture<Person> future = helloService.sayHelloAsync(new Name(name), 20L);
        future.thenAccept(person -> {
            System.out.println("Received message [" + count + "]：" + person);
        });
        System.out.println("This is [" + count + "] " + "waiting");
        // 继续非阻塞执行
    }
}
