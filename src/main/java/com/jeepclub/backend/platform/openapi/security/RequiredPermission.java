package com.jeepclub.backend.platform.openapi.security;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiredPermission {

    String[] value();
}