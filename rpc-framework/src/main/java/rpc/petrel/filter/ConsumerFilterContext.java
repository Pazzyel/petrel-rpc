package rpc.petrel.filter;

import rpc.petrel.annotation.Activate;
import rpc.petrel.enums.TypeEnum;
import rpc.petrel.extension.ExtensionLoader;

import java.util.ArrayList;
import java.util.List;

public class ConsumerFilterContext {
    private static final List<PetrelFilter> filters = new ArrayList<>();
    static {
        List<PetrelFilter> extensions = ExtensionLoader.getExtensionLoader(PetrelFilter.class).getAllExtensions();
        extensions.forEach((extension) -> {
            Activate annotation = extension.getClass().getAnnotation(Activate.class);
            if (annotation == null) {
                throw new IllegalArgumentException("Filter class " + extension.getClass().getName() + " is not annotated with @Activate.");
            }
            // 只有CONSUMER 类型的才加入
            if (annotation.group().equals(TypeEnum.CONSUMER)) {
                filters.add(extension);
            }
        });
        sortFilters();
    }
    public static void addFilter(PetrelFilter filter) {
        filters.add(filter);
    }
    public static void sortFilters() {
        filters.sort((o1, o2) -> {
            Activate annotation1 = o1.getClass().getAnnotation(Activate.class);
            Activate annotation2 = o2.getClass().getAnnotation(Activate.class);
            if (annotation1 == null || annotation2 == null) {
                throw new IllegalArgumentException("Filter class must be annotated with @Activate");
            }
            // order顺序不同时按照order值排序
            if (annotation1.order() != annotation2.order()) {
                return annotation1.order() - annotation2.order();
            } else {
                // 相同按照名字的字典序
                return o1.getClass().getName().compareTo(o2.getClass().getName());
            }
        });
    }

    public static void invoke(Invocation invocation) {
        filters.forEach(f -> f.invoke(invocation));
    }
}
