package com.jeepclub.backend.platform.web.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Clock;
import java.util.List;
import java.util.Locale;

@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ApiProblemFactory problemFactory;
    private final MessageSource messageSource;
    private final Clock clock;

    @Autowired(required = false)
    public GlobalExceptionHandler(
            ApiProblemFactory problemFactory,
            MessageSource messageSource,
            Clock clock
    ) {
        this.problemFactory = problemFactory;
        this.messageSource = messageSource;
        this.clock = clock;
    }

    public GlobalExceptionHandler() {
        this(Clock.systemUTC(), standaloneMessageSource());
    }

    private GlobalExceptionHandler(Clock clock, MessageSource messageSource) {
        this(new ApiProblemFactory(messageSource, clock), messageSource, clock);
    }

    private static MessageSource standaloneMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Locale locale = LocaleContextHolder.getLocale();
        List<ValidationFieldErrorResponse> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> toFieldErrorResponse(fieldError, locale))
                .toList();

        ApiErrorResponse problem = problemFactory.create(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                locale
        );
        problem.setErrors(errors);

        return handleExceptionInternal(
                exception,
                problem,
                headers,
                status,
                request
        );
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        if (body instanceof ProblemDetail problem) {
            if (!(problem instanceof ApiErrorResponse)
                    && (problem.getProperties() == null
                    || !problem.getProperties().containsKey("code"))) {
                String code = "HTTP_" + statusCode.value();
                Locale locale = LocaleContextHolder.getLocale();
                String fallbackMessage = messageSource.getMessage(
                        "api.error.GENERIC",
                        null,
                        "The request could not be completed.",
                        locale
                );
                problem.setDetail(messageSource.getMessage(
                        "api.error." + code,
                        null,
                        fallbackMessage,
                        locale
                ));
                problem.setTitle(messageSource.getMessage(
                        "api.status." + statusCode.value(),
                        null,
                        problem.getTitle(),
                        locale
                ));
                problem.setProperty("code", code);
            }

            if (!(problem instanceof ApiErrorResponse)) {
                problem.setProperty("timestamp", clock.instant());
            }
        }

        return super.createResponseEntity(body, headers, statusCode, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(
            Exception exception,
            Locale locale
    ) {
        log.error("Unhandled exception while processing the HTTP request", exception);
        return problemFactory.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                locale
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(
            IllegalArgumentException exception,
            Locale locale
    ) {
        return problemFactory.create(
                HttpStatus.BAD_REQUEST,
                "INVALID_ARGUMENT",
                locale
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(
            IllegalStateException exception,
            Locale locale
    ) {
        return problemFactory.create(
                HttpStatus.CONFLICT,
                "INVALID_STATE",
                locale
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(
            AccessDeniedException exception,
            Locale locale
    ) {
        return problemFactory.create(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                locale
        );
    }

    private ValidationFieldErrorResponse toFieldErrorResponse(
            FieldError fieldError,
            Locale locale
    ) {
        String constraintCode = fieldError.getCode() == null
                ? "Invalid"
                : fieldError.getCode();
        String message = messageSource.getMessage(
                "validation." + constraintCode.toLowerCase(Locale.ROOT),
                null,
                fieldError.getDefaultMessage(),
                locale
        );
        String responseCode = constraintCode
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase(Locale.ROOT);

        return new ValidationFieldErrorResponse(
                fieldError.getField(),
                responseCode,
                message
        );
    }
}

