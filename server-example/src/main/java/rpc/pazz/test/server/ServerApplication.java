package rpc.pazz.test.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import rpc.pazz.annotation.EnableRPC;
import rpc.pazz.annotation.RpcScan;

@EnableRPC
@SpringBootApplication
public class ServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}
