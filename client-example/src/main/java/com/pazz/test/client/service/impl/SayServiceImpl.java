package com.pazz.test.client.service.impl;

import com.pazz.test.client.service.SayService;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import rpc.pazz.annotation.RpcReference;
import rpc.pazz.test.api.HelloService;

@Service
public class SayServiceImpl implements SayService {

    @RpcReference
    private HelloService helloService;

    @Override
    public void say() {
        String name = "Pazz";
        String result = helloService.sayHello(name);
        System.out.println("Received message：" + result);
    }
}
