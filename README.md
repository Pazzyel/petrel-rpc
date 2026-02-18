# PetrelRPC

风燕RPC是一款用于微服务之间调用的RPC工具。使用少数几个注解就可以轻松完成RPC的服务注册，服务发现功能

本项目采用Zookeeper作为注册中心，不同服务之间通过Netty进行通信，内容使用Kryo算法序列化+Gzip算法压缩

## 注册中心

要使用PetrelRPC，需要有一个注册中心，本项目使用Zookeeper作为注册中心

如果之前没有接触过Zookeeper，可以先在docker创建一个

```bash
docker run -d --name zookeeper -p 2181:2181 zookeeper:3.8.5
```

请使用较新的版本，旧的版本（3.5.8）实测会出现连接不稳定的问题

在项目的`resource`目录下，创建文件`rpc.properties`，里面指定Zookeeper的连接地址。如果没有该配置内容，将使用默认地址`127.0.0.1:2181`

```properties
rpc.zookeeper.address=127.0.0.1:2181
```

## 在Spring Boot项目中使用

无论是服务注册还是服务发现，只要用到了相关功能，都必须在项目的启动类中加上`@EnableRPC`

```java
@EnableRPC
@SpringBootApplication
public class ServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}

```

只在需要进行服务注册的类上使用`@RpcService`，这将会注册其为Bean，可以不再需要在其上加上`@Service`

```java
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
```

在需要进行服务发现的字段上使用`@RpcReference`，这将会在Bean初始化时为其注入可以发起RPC调用的代理类实例。这个字段所在的类必须作为Bean被Spring管理，可以使用`@Service`

```java
@Slf4j
@Service
public class SayServiceImpl implements SayService {

    @RpcReference(group = "test1", version = "version1")
    private HelloService helloService;

    @Override
    public void say(int count) {
        String name = "Me";
        String result = helloService.sayHello(name);
        log.info("Received response message: {}", result);
    }
}
```

一对对应的`@RpcService`和`@RpcReference`的`group``version`一致时调用才能成功。不指定值时，默认为空字符串`""`

因为使用的JDK动态代理，通过`@RpcService`注册的服务必须实现一个接口，注入对应的`@RpcReference`字段的类型只能是这个接口



