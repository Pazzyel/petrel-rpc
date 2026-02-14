package com.pazz.test.client;

import com.pazz.test.client.service.SayService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import rpc.pazz.annotation.EnableRPC;

@EnableRPC(basePackage = {"com.pazz.test.client"})
@SpringBootApplication
public class ClientApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(ClientApplication.class, args);
        SayService sayService = ctx.getBean(SayService.class);
        sayService.say();
    }
}
