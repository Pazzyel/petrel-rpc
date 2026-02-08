package rpc.pazz.serialize.kyro;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import rpc.pazz.exception.SerializeException;
import rpc.pazz.remote.dto.RpcRequest;
import rpc.pazz.remote.dto.RpcResponse;
import rpc.pazz.serialize.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class KyroSerializer implements Serializer {

    //Kryo不是线程安全的，使用ThreadLocal解决并发安全性
    //Kryo is not thread-safe, use ThreadLocal
    private final ThreadLocal<Kryo> kyroThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.register(RpcRequest.class);
        kryo.register(RpcResponse.class);
        return kryo;
    });

    //序列化
    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Output output = new Output(byteArrayOutputStream)) {
            Kryo kryo = kyroThreadLocal.get();
            kryo.writeObject(output, obj);
            kyroThreadLocal.remove();
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
            Kryo kryo = kyroThreadLocal.get();
            Object o = kryo.readObject(input, clazz);
            kyroThreadLocal.remove();
            return clazz.cast(o);
        } catch (IOException e) {
            throw new SerializeException("Deserialize failed");
        }
    }
}
