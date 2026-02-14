package rpc.pazz.test.server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpc.pazz.annotation.RpcService;
import rpc.pazz.test.api.HelloService;

@Service
@Slf4j
@RpcService
public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
        log.info("Received request from: {}", name);
        return "Hello " + name;
    }
}
