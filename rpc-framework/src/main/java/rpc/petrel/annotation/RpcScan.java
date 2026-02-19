package rpc.petrel.annotation;

import org.springframework.context.annotation.Import;
import rpc.petrel.spring.CustomScannerRegistrar;

import java.lang.annotation.*;

@Deprecated
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Import(CustomScannerRegistrar.class)
@Documented
public @interface RpcScan {

    String[] basePackage();

}
