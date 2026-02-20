package rpc.petrel.serialize.kryo;

import java.util.List;
import java.util.Set;

public interface KryoClassRegistrar {
    void registerClasses(List<Class<?>> registry);
}
