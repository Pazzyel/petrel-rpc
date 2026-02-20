package rpc.petrel.test.server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rpc.petrel.annotation.RpcService;
import rpc.petrel.test.api.Age;
import rpc.petrel.test.api.HelloService;
import rpc.petrel.test.api.Name;
import rpc.petrel.test.api.Person;

@Service
@Slf4j
@RpcService(group = "test1", version = "version1")
public class HelloServiceImpl implements HelloService {

    @Override
    public Person sayHello(Name name, Long age) {
        log.info("Received request from: {}", name.getName());
        return new Person(name,new Age(Math.toIntExact(age)));
    }
}
