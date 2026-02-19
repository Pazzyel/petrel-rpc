package rpc.petrel.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class PropertyUtils {

    private static final Logger logger = LoggerFactory.getLogger(PropertyUtils.class);

    /**
     * 从path加载Properties
     * @param path properties文件路径
     * @return Properties对象
     */
    public static Properties loadProperties(String path) {
        Properties props = new Properties();
        ClassPathUtils.readInputStream(path, input -> {
            logger.atDebug().log("从{}加载了配置", path);
            props.load(input);
            return true;
        });
        return props;
    }
}
