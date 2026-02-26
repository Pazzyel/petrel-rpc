package rpc.petrel.annotation;

import rpc.petrel.enums.TypeEnum;

import java.lang.annotation.*;

/**
 * 指定拦截方法类
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Activate {
    TypeEnum group();

    int order() default 100;
}
