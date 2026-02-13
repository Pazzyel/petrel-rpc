package rpc.pazz.annotation;

import java.lang.annotation.*;

/**
 * 被注解的字段，将会生成代理类注册为Bean，对其的方法调用全部拦截并进行RPC调用
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
public @interface RpcReference {

    /**
     * Service version, default value is empty string
     */
    String version() default "";

    /**
     * Service group, default value is empty string
     */
    String group() default "";

}