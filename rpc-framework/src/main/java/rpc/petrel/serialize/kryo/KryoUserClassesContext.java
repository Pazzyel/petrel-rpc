package rpc.petrel.serialize.kryo;

import lombok.extern.slf4j.Slf4j;
import rpc.petrel.extension.ExtensionLoader;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class KryoUserClassesContext {

    private static Set<Class<?>> needRegister = ConcurrentHashMap.newKeySet();
    private static final String REGISTER_NAME = "register";

    static {
        // 尝试通过解析SPI加载注册类
        try {
            KryoClassRegistrar registrar = ExtensionLoader.getExtensionLoader(KryoClassRegistrar.class).getExtension(REGISTER_NAME);
            if (registrar != null) {
                registrar.registerClasses(needRegister);
            }
        } catch (RuntimeException e) {
            log.info("Kryo register SPI is empty, maybe register in Spring BeanFactoryPostProcessor");
        }
    }

    static Set<Class<?>> getNeedRegister() {
        return needRegister;
    }
}
