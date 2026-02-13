package rpc.pazz.annotation;

import java.lang.annotation.*;

/**
 * 被注解的Java类会成为Bean，将会作为服务被注册
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface RpcService {

    /**
     * Service version, default value is empty string
     */
    String version() default "";

    /**
     * Service group, default value is empty string
     */
    String group() default "";

}
