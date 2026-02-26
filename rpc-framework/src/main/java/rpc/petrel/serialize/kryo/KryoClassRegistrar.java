package rpc.petrel.serialize.kryo;

import rpc.petrel.extension.SPI;

import java.util.Set;

@SPI
public interface KryoClassRegistrar {
    void registerClasses(Set<Class<?>> registry);
}
