package rpc.petrel.filter;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import rpc.petrel.annotation.Activate;
import rpc.petrel.enums.TypeEnum;

import java.util.Map;

public class FilterRegistrarBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // 获取用户的注册实现类Bean，此时没有任何除了BeanFactoryPostProcessor的Bean被实例化
        Map<String, PetrelFilter> registrars = beanFactory.getBeansOfType(PetrelFilter.class);
        for (PetrelFilter f : registrars.values()) {
            Activate annotation = f.getClass().getAnnotation(Activate.class);
            if (annotation == null) {
                throw new RuntimeException("Filter class must be annotated with @Activate");
            }
            if (annotation.group().equals(TypeEnum.PROVIDER)) {
                ProviderFilterContext.addFilter(f);
            } else {
                ConsumerFilterContext.addFilter(f);
            }
        }
        ProviderFilterContext.sortFilters();
        ConsumerFilterContext.sortFilters();
    }
}
