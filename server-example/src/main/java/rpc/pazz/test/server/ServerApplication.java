package rpc.pazz.test.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import rpc.pazz.annotation.EnableRPC;

@EnableRPC(basePackage = {"rpc.pazz.test.server"})
@SpringBootApplication
public class ServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
