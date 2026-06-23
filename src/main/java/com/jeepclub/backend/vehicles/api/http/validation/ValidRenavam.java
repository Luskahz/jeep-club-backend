package com.jeepclub.backend.vehicles.api.http.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RenavamValidator.class)
public @interface ValidRenavam {
    String message() default "Código RENAVAM inválido.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}