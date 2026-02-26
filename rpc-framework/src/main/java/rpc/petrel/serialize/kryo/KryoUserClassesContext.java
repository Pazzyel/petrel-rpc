package rpc.petrel.serialize.kryo;

import java.util.HashSet;
import java.util.Set;

public class KryoUserClassesContext {

    // 并发只发生在读过程,没必要用线程安全类
    private static Set<Class<?>> needRegister = new HashSet<>();

    static Set<Class<?>> getNeedRegister() {
        return needRegister;
    }
}
