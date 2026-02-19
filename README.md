# Petrel RPC

Petrel RPC是一款用于微服务之间调用的RPC工具。使用少数几个注解就可以轻松完成RPC的服务注册，服务发现功能

本项目采用Zookeeper作为注册中心，不同服务之间通过Netty进行通信，内容使用Kryo算法序列化+Gzip算法压缩

## 注册中心

要使用PetrelRPC，需要有一个注册中心，本项目使用Zookeeper作为注册中心

如果之前没有接触过Zookeeper，可以先在docker创建一个

```bash
docker run -d --name zookeeper -p 2181:2181 zookeeper:3.8.5
```

请使用较新的版本，旧的版本（3.5.8）实测会出现连接不稳定的问题

在项目的`resource`目录下，在配置文件`application.yaml`，里面指定Zookeeper的连接地址。如果没有该配置内容，将使用默认地址`127.0.0.1:2181`

你还可以指定其它的相关配置，下面的是所有可配置项的默认值

```yaml
petrel:
  registry:
    type: zookeeper
    address: localhost:2181
  connection: netty
  serializer: kryo
  compression: gzip
```

在`rpc.petrel.properties.PropertyResolver`定义了会加载的配置文件的名称。

配置文件将以`application.yaml`->`application.yml`->`application.properties`的顺序解析，只有当前解析的配置文件每次不存在，才会查找按顺序的下一个文件。多个不同配置文件只会按优先级加载最高的那个。

配置文件的这一部分配置读取，并不是使用Spring Boot来进行的，因此通过Spring Boot提供的`spring.profiles.active`来指定活跃的其它配置文件（如`application-dev.yaml`）是无效的。配置只能写在`application.yaml/application.yml/application.properties`中。

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

## 配置的其它可选项

### connection

- netty: 默认情况下使用Netty进行网络通信，充分利用NIO模型的特点，使用少量的线程来处理多个连接的通信，提高了I/O效率和并发
- socket: 可以先择使用Java原生Socket，因为服务端采用了JDK21的虚拟线程，避免了传统OS线程创建和阻塞成本高的问题。维护成本较低且性能和netty不会有非常明显的差异

