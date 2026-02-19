package rpc.petrel.properties;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//用于加载yaml配置文件
public class YamlUtils {

    static class NoImplicitResolver extends Resolver {
        public NoImplicitResolver() {
            super();
            super.yamlImplicitResolvers.clear();
        }
    }

    /**
     * 把yaml转换成Map<String,Object>注意其为树形结构
     * @param path yaml文件的路径
     * @return 树形结构的Map
     */
    public static Map<String,Object> loadYaml(String path){
        LoaderOptions loaderOptions = new LoaderOptions();
        DumperOptions dumperOptions = new DumperOptions();
        Representer representer = new Representer(dumperOptions);
        NoImplicitResolver resolver = new NoImplicitResolver();
        Yaml yaml = new Yaml(new Constructor(loaderOptions), representer, dumperOptions, loaderOptions, resolver);
        return ClassPathUtils.readInputStream(path, stream -> (Map<String, Object>) yaml.load(stream));
    }

    /**
     * 把yaml转换成扁平Map<String,Object>
     * @param path yaml文件的路径
     * @return 扁平结构的Map
     */
    public static Map<String,String> loadPlainYaml(String path){
        Map<String,Object> treeYaml = loadYaml(path);
        //扁平结构的Map可能有重复的键，必须用LinkedHashMap
        Map<String,String> plainYaml = new LinkedHashMap<>();
        convertTreeToPlainMap(treeYaml,"",plainYaml);
        return plainYaml;
    }

    /**
     * 把树形Map转换成扁平Map
     * @param source 树形Map
     * @param prefix 递归转换中先前key的前缀，一定以.结尾
     * @param target 扁平Map
     */
    private static void convertTreeToPlainMap(Map<String,Object> source, String prefix, Map<String,String> target) {
        for (String key : source.keySet()) {
            Object value = source.get(key);
            if (value instanceof Map) {
                Map<String,Object> map = (Map<String,Object>) value;
                convertTreeToPlainMap(map, prefix + key + ".", target);
            } else if (value instanceof List) {
                ((List<?>) value).forEach(e -> target.put(prefix + key,e.toString()));
            } else {
                target.put(prefix + key, value.toString());//SnakeYaml默认会自动转换int、boolean等value，需要手动把value均按String类型返回
            }
        }
    }
}
