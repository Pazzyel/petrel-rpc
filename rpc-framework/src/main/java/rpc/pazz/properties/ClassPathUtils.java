package rpc.pazz.properties;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

//用于查找指定路径下resource的输入流并对齐执行方法
public class ClassPathUtils {


    /**
     * 用于查找指定路径下resource的输入流并对齐执行callback方法
     * @param path 查找路径
     * @param callback 对输入流的回调方法
     * @return callback的返回结果
     * @param <T> callback的返回类型
     */
    public static <T> T readInputStream(String path, InputStreamCallback<T> callback) {
        //移除开头的/
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        //对path路径下的输入流执行callback
        try (InputStream stream = ClassPathUtils.getContextClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new FileNotFoundException("File not found: " + path);
            }
            return callback.doWithInputStream(stream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }

    }

    private static ClassLoader getContextClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl != null ? cl : ClassPathUtils.class.getClassLoader();
    }
}
