package rpc.petrel.serialize.kryo;

import java.util.ArrayList;
import java.util.List;

public class KryoUserClassesContainer {

    // 并发只发生在读过程,没必要用线程安全类
    private static List<Class<?>> needRegister = new ArrayList<>();

    static List<Class<?>> getNeedRegister() {
        return needRegister;
    }
}
