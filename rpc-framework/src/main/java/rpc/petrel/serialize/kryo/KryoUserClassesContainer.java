package rpc.petrel.serialize.kryo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class KryoUserClassesContainer {

    // 并发只发生在读过程，好像也没必要用Concurrent ?
    static Set<Class<?>> needRegister = ConcurrentHashMap.newKeySet();

    static Set<Class<?>> getNeedRegister() {
        return needRegister;
    }
}
