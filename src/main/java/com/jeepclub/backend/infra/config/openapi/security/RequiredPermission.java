package com.jeepclub.backend.infra.config.openapi.security;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiredPermission {

    String[] value();
}