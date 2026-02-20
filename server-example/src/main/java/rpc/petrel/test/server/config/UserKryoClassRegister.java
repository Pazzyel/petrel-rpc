package rpc.petrel.test.server.config;

import org.springframework.stereotype.Component;
import rpc.petrel.serialize.kryo.KryoClassRegistrar;
import rpc.petrel.test.api.Age;
import rpc.petrel.test.api.Name;
import rpc.petrel.test.api.Person;

import java.util.List;

@Component
public class UserKryoClassRegister implements KryoClassRegistrar {
    @Override
    public void registerClasses(List<Class<?>> registry) {
        registry.add(Person.class);
        registry.add(Name.class);
        registry.add(Age.class);
    }
}
