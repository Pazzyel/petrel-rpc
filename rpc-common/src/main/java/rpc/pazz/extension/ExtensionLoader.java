package rpc.pazz.extension;

import lombok.extern.slf4j.Slf4j;
import rpc.pazz.factory.SingletonFactory;
import rpc.pazz.utils.StringUtil;

import javax.imageio.IIOException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ExtensionLoader<T> {

    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";

    /**
     * 扩展类加载器的缓存，每一个类都有一个扩展类加载器。
     * 需要考虑多线程的问题
     * */
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();


    private final Class<?> type;

    /**
     * 实例缓存，根据名字进行缓存
     * 保证可见性的 Holder。
     * */
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

    /**
     * 类缓存，根据名称进行缓存，是从文件中进行读取的key，value，只需要从文件初始化一次
     * */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }

    //获取类加载器的工厂方法
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Extension type should not be null.");
        }
        if (!clazz.isInterface()) {
            // 需要是接口
            throw new IllegalArgumentException("Extension type must be an interface.");
        }
        if (clazz.getAnnotation(SPI.class) == null) {
            // 类上需要包含SPI注解
            throw new IllegalArgumentException("Extension type must be annotated by @SPI");
        }
        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(clazz);
        if (extensionLoader == null) {
            EXTENSION_LOADERS.putIfAbsent(clazz, new ExtensionLoader<>(clazz));
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(clazz);
        }
        return extensionLoader;
    }

    public T getExtension(String name) {
        if (StringUtil.isBlank(name)) {
            throw new IllegalArgumentException("Extension name should not be null or empty.");
        }
        Holder<Object> holder = cachedInstances.get(name);
        //没有就先创建Holder对象
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        //单例模式创建工厂对象
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        
        return (T) instance;
    }

    //创建扩展类实例
    private T createExtension(String name) {
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new RuntimeException("Extension class " + name + " not found.");
        }
        //获取class后，利用工厂类创建单例
        return (T) SingletonFactory.getInstance(clazz);
    }

    //获取该扩展类加载器配置的所有class的Map，如果没有，读取文件并加载
    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            //没有扩展类，就加锁并从配置文件文件加载出扩展类
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = new HashMap<>();
                    loadDirectory(classes);//从配置文件文件加载出扩展类，放入Map
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    //拼接文件地址并读取文件
    private void loadDirectory(Map<String, Class<?>> extensionClasses) {
        //配置文件路径
        String fileName = SERVICE_DIRECTORY + type.getName();
        try {
            //加载配置文件
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            Enumeration<URL> urls = classLoader.getResources(fileName);//获取配置文件路径
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    loadResource(extensionClasses,classLoader,url);//从配置文件路径加载配置
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    //读取文件内容并放入扩展类加载中的class的Map
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL url) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                //去掉‘#’后的注释部分
                int commentIndex = line.indexOf('#');
                if (commentIndex >= 0) {
                    line = line.substring(0, commentIndex);
                }
                //去掉首尾空格
                line = line.trim();
                if (!StringUtil.isBlank(line)) {
                    try {
                        //解析名字和扩展类的实现类名字
                        int equalsIndex = line.indexOf('=');
                        String name = line.substring(0, equalsIndex).trim();
                        String className = line.substring(equalsIndex + 1).trim();
                        if (!StringUtil.isBlank(name) && !StringUtil.isBlank(className)) {
                            Class<?> clazz = classLoader.loadClass(className);
                            extensionClasses.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
