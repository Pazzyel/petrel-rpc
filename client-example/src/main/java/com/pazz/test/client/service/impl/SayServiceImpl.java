package com.pazz.test.client.service.impl;

import com.pazz.test.client.service.SayService;
import org.springframework.stereotype.Service;
import rpc.petrel.annotation.RpcReference;
import rpc.petrel.test.api.HelloService;

@Service
public class SayServiceImpl implements SayService {

    @RpcReference(group = "test1", version = "version1")
    private HelloService helloService;

    @Override
    public void say(int count) {
        String name = "Pazz";
        String result = helloService.sayHello(name);
        System.out.println("Received message [" + count + "]：" + result);
    }
}
