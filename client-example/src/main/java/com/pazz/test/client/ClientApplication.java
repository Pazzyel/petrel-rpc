package com.pazz.test.client;

import com.pazz.test.client.service.SayService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import rpc.pazz.annotation.EnableRPC;
import rpc.pazz.annotation.RpcScan;

@EnableRPC
@SpringBootApplication
public class ClientApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(ClientApplication.class, args);
        SayService sayService = ctx.getBean(SayService.class);
        for (int i = 0; i < 10; i++) {
            sayService.say(i);
        }
        try {
            Thread.sleep(15 * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (int i = 10; i < 20; i++) {
            sayService.say(i);
        }
    }
}
