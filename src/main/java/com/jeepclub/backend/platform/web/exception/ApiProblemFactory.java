package com.jeepclub.backend.platform.web.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ApiProblemFactory {

    private static final String ERROR_MESSAGE_PREFIX = "api.error.";
    private static final String STATUS_TITLE_PREFIX = "api.status.";

    private final MessageSource messageSource;
    private final Clock clock;

    public ApiErrorResponse create(
            HttpStatus status,
            String code,
            Locale locale,
            Object... arguments
    ) {
        return create(
                status,
                code,
                ERROR_MESSAGE_PREFIX + code,
                locale,
                arguments
        );
    }

    public ApiErrorResponse create(
            HttpStatus status,
            String code,
            String messageKey,
            Locale locale,
            Object... arguments
    ) {
        String fallbackMessage = messageSource.getMessage(
                ERROR_MESSAGE_PREFIX + "GENERIC",
                null,
                code,
                locale
        );
        String detail = messageSource.getMessage(
                messageKey,
                arguments,
                fallbackMessage,
                locale
        );
        String title = messageSource.getMessage(
                STATUS_TITLE_PREFIX + status.value(),
                null,
                status.getReasonPhrase(),
                locale
        );

        return ApiErrorResponse.localized(
                status,
                title,
                detail,
                code,
                clock.instant()
        );
    }
}
