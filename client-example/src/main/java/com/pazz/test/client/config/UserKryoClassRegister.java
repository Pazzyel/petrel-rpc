package com.pazz.test.client.config;

import rpc.petrel.serialize.kryo.KryoClassRegistrar;
import rpc.petrel.test.api.Age;
import rpc.petrel.test.api.Name;
import rpc.petrel.test.api.Person;

import java.util.Set;

public class UserKryoClassRegister implements KryoClassRegistrar {
    @Override
    public void registerClasses(Set<Class<?>> registry) {
        registry.add(Person.class);
        registry.add(Name.class);
        registry.add(Age.class);
    }
}
