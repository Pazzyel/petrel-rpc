package com.pazz.test.client.service.impl;

import com.pazz.test.client.service.SayService;
import org.springframework.stereotype.Service;
import rpc.petrel.annotation.RpcReference;
import rpc.petrel.async.RpcFuture;
import rpc.petrel.test.api.HelloService;

@Service
public class SayServiceImpl implements SayService {

    @RpcReference(group = "test1", version = "version1")
    private HelloService helloService;

    @Override
    public void say(int count) {
        String name = "Pazz";
        RpcFuture<String> future = helloService.sayHelloAlso(name);
        System.out.println("This is [" + count + "] " + "waiting");
        String result = future.get();
        System.out.println("Received message [" + count + "]：" + result);
    }
}
