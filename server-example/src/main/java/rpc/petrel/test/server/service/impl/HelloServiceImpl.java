package rpc.petrel.test.server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpc.petrel.annotation.RpcService;
import rpc.petrel.test.api.HelloService;

@Service
@Slf4j
@RpcService(group = "test1", version = "version1")
public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
        log.info("Received request from: {}", name);
        return "Hello " + name;
    }
}
