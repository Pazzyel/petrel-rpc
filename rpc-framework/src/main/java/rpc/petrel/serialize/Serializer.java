package rpc.petrel.serialize;

import rpc.petrel.extension.SPI;

@SPI
public interface Serializer {

    /**
     * 序列化
     * @param obj 等待序列化对象
     * @return 序列化后字节流
     */
    byte[] serialize(Object obj);

    /**
     * 反序列化
     * @param bytes 等待反序列化字节流
     * @param clazz 目标类
     * @return 反序列化对象
     * @param <T> 目标类类型
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
