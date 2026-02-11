package rpc.pazz.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ThreadPoolFactoryUtil {

    public static ThreadFactory createThreadFactory(String threadNamePrefix, Boolean isDaemon) {
        if (threadNamePrefix != null) {
            String name = threadNamePrefix + "-%d";
            if (isDaemon != null) {
                return new ThreadFactoryBuilder().setDaemon(isDaemon).setNameFormat(name).build();
            } else {
                return new ThreadFactoryBuilder().setNameFormat(name).build();
            }
        } else {
            return Executors.defaultThreadFactory();
        }
    }
}
