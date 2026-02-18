package rpc.pazz.factory;

import rpc.pazz.extension.Holder;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SingletonFactory {
    private static final Map<String, Holder<Object>> OBJECT_MAP = new ConcurrentHashMap<>();//保存唯一实例
    //private static final Object lock = new Object();//并发锁对象
    private static final ReentrantLock lock = new ReentrantLock();

    private SingletonFactory() {
    }

    public static <T> T getInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        String key = clazz.getName();

        Holder<Object> holder = OBJECT_MAP.get(key);
        if (holder != null && holder.get() != null) {
            return clazz.cast(holder.get());
        }

        //synchronized (lock) {
        lock.lock();
        try {
            //锁范围内再查一次，防止其它线程已经创建
            holder = OBJECT_MAP.computeIfAbsent(key, k -> new Holder<>());
            if (holder.get() == null) {
                try {
                    Constructor<T> constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    T instance = constructor.newInstance();
                    holder.set(instance);//不用put回去，因为本来就是从map查出来的
                } catch (Exception e) {
                    throw new RuntimeException("创建实例失败", e);
                }
            }
        } finally {
            lock.unlock();
        }
        //}

        return clazz.cast(holder.get());//在之前已经被替换为map查出来的对象
    }

    public static <T> T getInstance(Supplier<T> supplier, Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        String key = clazz.getName();

        Holder<Object> holder = OBJECT_MAP.get(key);
        if (holder != null && holder.get() != null) {
            return clazz.cast(holder.get());
        }

        //synchronized (lock) {
        lock.lock();
        try {
            //锁范围内再查一次，防止其它线程已经创建
            holder = OBJECT_MAP.computeIfAbsent(key, k -> new Holder<>());
            if (holder.get() == null) {
                try {
                    T instance = supplier.get();//使用生产者创建
                    holder.set(instance);//不用put回去，因为本来就是从map查出来的
                } catch (Exception e) {
                    throw new RuntimeException("创建实例失败", e);
                }
            }
        } finally {
            lock.unlock();
        }
        //}

        return clazz.cast(holder.get());//在之前已经被替换为map查出来的对象
    }

    public static <T> T getInstance(Consumer<T> consumer, Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz must not be null");
        }
        String key = clazz.getName();

        Holder<Object> holder = OBJECT_MAP.get(key);
        if (holder != null && holder.get() != null) {
            return clazz.cast(holder.get());
        }

        //synchronized (lock) {
        lock.lock();
        try {
            //锁范围内再查一次，防止其它线程已经创建
            holder = OBJECT_MAP.computeIfAbsent(key, k -> new Holder<>());
            if (holder.get() == null) {
                try {
                    Constructor<T> constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    T instance = constructor.newInstance();
                    consumer.accept(instance);//放入消费者
                    holder.set(instance);//不用put回去，因为本来就是从map查出来的
                } catch (Exception e) {
                    throw new RuntimeException("创建实例失败", e);
                }
            }
        } finally {
            lock.unlock();
        }
        //}

        return clazz.cast(holder.get());//在之前已经被替换为map查出来的对象
    }
}

