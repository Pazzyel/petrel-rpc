package rpc.pazz.properties;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.time.*;
import java.util.*;
import java.util.function.Function;

/**
 * 配置文件读取
 */
public class PropertyResolver {
    //配置的key-value，key是xxx.xxx的形式
    private final Map<String,String> properties = new HashMap<>();
    //转换器的Class<?>-Function<String,Object>,用于获取对应类型的转换器
    private final Map<Class<?>,Function<String,Object>> converters = new HashMap<>();

    public static final String CONFIG_APP_YAML = "application.yaml";
    public static final String CONFIG_APP_YML = "application.yml";
    public static final String CONFIG_APP_PROP = "application.properties";


    private PropertyResolver() {}

    /**
     * 从classpath下查找配置文件，按照yaml->yml->properties的顺序解析
     * @return PropertyResolver对象
     */
    public static PropertyResolver createPropertyResolver() {
        PropertyResolver resolver = null;
        try {
            resolver = new PropertyResolver(CONFIG_APP_YAML);
        } catch (UncheckedIOException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                try {
                    resolver = new PropertyResolver(CONFIG_APP_YML);
                } catch (UncheckedIOException e1) {
                    if (e1.getCause() instanceof FileNotFoundException) {
                        resolver = new PropertyResolver(PropertyUtils.loadProperties(CONFIG_APP_PROP));
                    }
                }
            }
        }
        return resolver;
    }

    /**
     * 加载properties形式的配置文件
     * @param props properties
     */
    public PropertyResolver(Properties props) {
        //存入环境变量
        properties.putAll(System.getenv());
        //存入传入的配置参数
        Set<String> keys = props.stringPropertyNames();
        for (String key : keys) {
            properties.put(key, props.getProperty(key));
        }

        setAllConverters();
    }

    /**
     * 加载yaml形式的配置文件
     * @param yamlPath yaml文件路径
     */
    public PropertyResolver(String yamlPath) {
        properties.putAll(System.getenv());
        properties.putAll(YamlUtils.loadPlainYaml(yamlPath));
        setAllConverters();
    }

    private void setAllConverters() {
        //设置所有基本类型映射
        converters.put(byte.class,Byte::parseByte);
        converters.put(Byte.class, Byte::parseByte);
        converters.put(short.class,Short::parseShort);
        converters.put(Short.class, Short::parseShort);
        converters.put(int.class,Integer::parseInt);
        converters.put(Integer.class, Integer::parseInt);
        converters.put(long.class,Long::parseLong);
        converters.put(Long.class, Long::parseLong);
        converters.put(float.class,Float::parseFloat);
        converters.put(Float.class, Float::parseFloat);
        converters.put(double.class,Double::parseDouble);
        converters.put(Double.class, Double::parseDouble);
        converters.put(boolean.class,Boolean::parseBoolean);
        converters.put(Boolean.class, Boolean::parseBoolean);
        converters.put(String.class, s -> s);
        //设置时间日期地点类映射
        converters.put(LocalDate.class, LocalDate::parse);
        converters.put(LocalTime.class, LocalTime::parse);
        converters.put(LocalDateTime.class, LocalDateTime::parse);
        converters.put(ZonedDateTime.class, ZonedDateTime::parse);
        converters.put(Duration.class, Duration::parse);
        converters.put(ZoneId.class, ZoneId::of);
    }

    //为用户提供自定义类型转换接口
    public void registerConverter(Class<?> clazz, Function<String,Object> converter) {
        converters.put(clazz,converter);
    }

    /**
     * 获取配置的核心方法
     * @param key 配置的key，可以没有解析
     * @return 配置值的String
     */
    public String getProperty(String key) {
        Property property = parseProperty(key);
        if (property != null) {
            //是${}的形式
            String defaultValue = property.defaultValue();
            if (defaultValue != null) {
                return getProperty(property.key(), defaultValue);
            } else {
                return getNotNullValue(property.key());
            }
        }
        //是去掉${}的形式
        return properties.get(key);
    }

    //获取带有默认值的配置，key一定是不带${}的形式
    private String getProperty(String realKey, String defaultValue) {
        String value = properties.getOrDefault(realKey, defaultValue);
        return parseValue(value);
    }

    //value可能是真正的value，也可能是嵌套的带${}的形式的key
    private String parseValue(String value) {
        Property parsedValue = parseProperty(value);
        if (parsedValue != null) {
            //是${}的形式
            String defaultValue = parsedValue.defaultValue();
            if (defaultValue != null) {
                return getProperty(parsedValue.key(), defaultValue);
            } else {
                return getNotNullValue(parsedValue.key());
            }
        } else {
            //是真正的value
            return value;
        }
    }

    private Property parseProperty(String key) {
        if (key.startsWith("${") && key.endsWith("}")) {
            int index = key.indexOf(":");
            if (index == -1) {
                return new Property(key.substring(2, key.length() - 1), null);
            } else {
                return new Property(key.substring(2, index), key.substring(index + 1, key.length() - 1));
            }
        }
        return null;
    }

    //对于没有默认值的${}，必须取出不是null的value
    private String getNotNullValue(String key) {
        String value = getProperty(key);
        return Objects.requireNonNull(value, "Missing required property: " + key);
    }


    /**
     * 找到key指定类型的value
     * @param key 原始key
     * @param type 需要的类型
     * @return 指定类型的value
     * @param <T> value的类型
     */
    public <T> T getProperty(String key, Class<T> type) {
        String value = getProperty(key);
        return convert(type, value);
    }

    //把读取的String value值转换成对应类型
    private <T> T convert(Class<T> type, String value) {
        Function<String,Object> converter = converters.get(type);
        if (converter == null) {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
        return (T) converter.apply(value);
    }

    /**
     * 获取String类型的配置，不会是null
     * @param key 原始key
     * @return 配置的值
     */
    public String getRequiredProperty(String key) {
        String value = getProperty(key);
        return Objects.requireNonNull(value, "配置" + key + "没有找到");
    }

    /**
     * 获取指定类型的配置，不会是null
     * @param key 配置的key，可以解析
     * @param clazz 配置的类型
     * @return 配置的值
     * @param <T> 配置的类型
     */
    public <T> T getRequiredProperty(String key, Class<T> clazz) {
        T value = getProperty(key, clazz);
        return Objects.requireNonNull(value, "配置" + key + "没有找到");
    }
}
