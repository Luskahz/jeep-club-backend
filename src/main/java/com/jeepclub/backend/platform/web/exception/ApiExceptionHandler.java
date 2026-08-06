package com.jeepclub.backend.platform.web.exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class ApiExceptionHandler implements Ordered {

    private ApiProblemFactory problemFactory;

    @Autowired(required = false)
    final void configureProblemFactory(ApiProblemFactory problemFactory) {
        this.problemFactory = problemFactory;
    }

    @Override
    public final int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    protected final ResponseEntity<ApiErrorResponse> buildErrorResponse(
            String code,
            String fallbackMessage,
            HttpStatus status
    ) {
        if (problemFactory != null) {
            return ResponseEntity.status(status).body(
                    problemFactory.create(
                            status,
                            code,
                            LocaleContextHolder.getLocale()
                    )
            );
        }

        // Suporte a testes MVC standalone, executados sem injeção do Spring.
        return ResponseEntity.status(status).body(
                ApiErrorResponse.of(code, fallbackMessage, status)
        );
    }
}
