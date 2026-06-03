package com.jeepclub.backend.platform.openapi.group;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SwaggerOperationGroup {

    String value();

    int order() default 100;
}