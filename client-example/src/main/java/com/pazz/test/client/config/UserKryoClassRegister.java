package com.pazz.test.client.config;

import org.springframework.stereotype.Component;
import rpc.petrel.serialize.kryo.KryoClassRegistrar;
import rpc.petrel.test.api.Person;

import java.util.Set;

@Component
public class UserKryoClassRegister implements KryoClassRegistrar {
    @Override
    public void registerClasses(Set<Class<?>> registry) {
        registry.add(Person.class);
    }
}
