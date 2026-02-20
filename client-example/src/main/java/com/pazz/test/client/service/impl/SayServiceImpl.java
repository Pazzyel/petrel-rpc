package com.pazz.test.client.service.impl;

import com.pazz.test.client.service.SayService;
import org.springframework.stereotype.Service;
import rpc.petrel.annotation.RpcReference;
import rpc.petrel.async.RpcFuture;
import rpc.petrel.test.api.HelloService;
import rpc.petrel.test.api.Name;
import rpc.petrel.test.api.Person;

import java.util.concurrent.Future;

@Service
public class SayServiceImpl implements SayService {

    @RpcReference(group = "test1", version = "version1")
    private HelloService helloService;

    @Override
    public void say(int count) {
        String name = "Pazz";
        Future<Person> future = helloService.sayHelloAsync(new Name(name), 20L);
        System.out.println("This is [" + count + "] " + "waiting");
        Person result = null;
        try {
            result = future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Received message [" + count + "]：" + result);
    }
}
