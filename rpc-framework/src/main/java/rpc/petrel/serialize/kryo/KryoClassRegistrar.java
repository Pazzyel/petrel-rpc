package rpc.petrel.serialize.kryo;

import java.util.List;

public interface KryoClassRegistrar {
    void registerClasses(List<Class<?>> registry);
}
