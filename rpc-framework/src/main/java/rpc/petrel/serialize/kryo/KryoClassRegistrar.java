package rpc.petrel.serialize.kryo;

import java.util.Set;

public interface KryoClassRegistrar {
    void registerClasses(Set<Class<?>> registry);
}
