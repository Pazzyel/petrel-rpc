package rpc.petrel.serialize.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import rpc.petrel.exception.SerializeException;
import rpc.petrel.factory.SingletonFactory;
import rpc.petrel.properties.RpcProperties;
import rpc.petrel.remote.dto.RpcRequest;
import rpc.petrel.remote.dto.RpcResponse;
import rpc.petrel.serialize.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class KryoSerializer implements Serializer {

    private final RpcProperties properties = SingletonFactory.getInstance(RpcProperties.class);
    private final Boolean kryoRegistration = properties.getKryoRegistration();
    //Kryo不是线程安全的，使用ThreadLocal解决并发安全性
    //Kryo is not thread-safe, use ThreadLocal
    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        if (kryoRegistration) {
            log.info("Registering classes to Kryo");
            kryo.register(RpcRequest.class);
            kryo.register(RpcResponse.class);
            kryo.register(java.lang.Class[].class);
            kryo.register(java.lang.Class.class);
            kryo.register(java.lang.Object[].class);
            // 这会作为单例加载，也就是说，同样不会有并发安全问题
            List<Class<?>> userClasses = new ArrayList<>(KryoUserClassesContext.getNeedRegister());
            userClasses.sort(Comparator.comparing(Class::getName)); // 必须保证无论用户怎么注册，最后注册到Kryo的顺序都一样
            // 添加用户自定义注册类
            for (Class<?> c : userClasses) {
                kryo.register(c);
            }
        } else {
            kryo.setRegistrationRequired(false);
        }
        return kryo;
    });

    //序列化
    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Output output = new Output(byteArrayOutputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            kryo.writeObject(output, obj);
            //kryoThreadLocal.remove();
            output.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new SerializeException("Serialize failed");
        }
    }

    //反序列化
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             Input input = new Input(byteArrayInputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            Object o = kryo.readObject(input, clazz);
            //kryoThreadLocal.remove();
            return clazz.cast(o);
        } catch (IOException e) {
            throw new SerializeException("Deserialize failed");
        }
    }
}
