package rpc.petrel.annotation;

import org.springframework.context.annotation.Import;
import rpc.petrel.spring.RpcBeanConfiguration;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RpcBeanConfiguration.class)
public @interface EnableRPC {
}
