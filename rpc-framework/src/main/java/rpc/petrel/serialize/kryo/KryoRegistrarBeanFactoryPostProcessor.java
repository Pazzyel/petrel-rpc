package rpc.petrel.serialize.kryo;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.Map;

public class KryoRegistrarBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // 获取用户的注册实现类Bean，此时没有任何除了BeanFactoryPostProcessor的Bean被实例化
        Map<String, KryoClassRegistrar> registrars = beanFactory.getBeansOfType(KryoClassRegistrar.class);
        for (KryoClassRegistrar r : registrars.values()) {
            // 调用对应方法，将会注册用户的Class
            r.registerClasses(KryoUserClassesContainer.getNeedRegister());
        }
    }

}