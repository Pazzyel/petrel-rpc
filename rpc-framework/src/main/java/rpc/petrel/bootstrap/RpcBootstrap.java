package rpc.petrel.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import rpc.petrel.annotation.RpcService;
import rpc.petrel.extension.ExtensionLoader;
import rpc.petrel.factory.SingletonFactory;
import rpc.petrel.properties.RpcProperties;
import rpc.petrel.remote.transport.RpcServer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class RpcBootstrap implements ApplicationListener<ContextRefreshedEvent> {

    private final AtomicBoolean serverStarted = new AtomicBoolean(false);

    private final RpcProperties properties = SingletonFactory.getInstance(RpcProperties.class);

    private final RpcServer rpcServer = ExtensionLoader.getExtensionLoader(RpcServer.class).getExtension(properties.getServerType().getName());

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        //寻找被@RpcService注解的Bean
        Map<String, Object> rpcServiceBeans = event.getApplicationContext().getBeansWithAnnotation(RpcService.class);
        //没有被@RpcService注解的Bean，直接返回，不需要启动server
        if (rpcServiceBeans.isEmpty()) {
            return;
        }
        if (!serverStarted.compareAndSet(false, true)) {
            return;
        }
        Thread serverThread = new Thread(rpcServer::start, "rpc-server");
        serverThread.setDaemon(false);
        serverThread.start();
        log.info("Detected [{}] @RpcService bean(s), RPC server started automatically.", rpcServiceBeans.size());
    }
}

