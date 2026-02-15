package rpc.pazz.annotation;

import org.springframework.context.annotation.Import;
import rpc.pazz.spring.CustomScannerRegistrar;
import rpc.pazz.spring.RpcBeanConfiguration;

import java.lang.annotation.*;

@Deprecated
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Import(CustomScannerRegistrar.class)
@Documented
public @interface RpcScan {

    String[] basePackage();

}
