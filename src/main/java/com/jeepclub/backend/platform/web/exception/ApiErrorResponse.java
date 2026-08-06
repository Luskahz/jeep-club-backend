package com.jeepclub.backend.platform.web.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.time.Instant;
import java.util.List;

/**
 * Tipo concreto usado também pela documentação OpenAPI. O JSON segue o
 * contrato RFC 9457 de {@link ProblemDetail}, acrescido de code e timestamp.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiErrorResponse extends ProblemDetail {

    private final String code;
    private final Instant timestamp;
    private List<ValidationFieldErrorResponse> errors;

    private ApiErrorResponse(
            HttpStatus status,
            String title,
            String detail,
            String code,
            Instant timestamp
    ) {
        super(status.value());
        this.code = code;
        this.timestamp = timestamp;
        setTitle(title);
        setDetail(detail);
    }

    public String getCode() {
        return code;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public List<ValidationFieldErrorResponse> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationFieldErrorResponse> errors) {
        this.errors = List.copyOf(errors);
    }

    static ApiErrorResponse localized(
            HttpStatus status,
            String title,
            String detail,
            String code,
            Instant timestamp
    ) {
        return new ApiErrorResponse(status, title, detail, code, timestamp);
    }

    public static ApiErrorResponse of(
            String code,
            String detail,
            HttpStatus status
    ) {
        return localized(
                status,
                status.getReasonPhrase(),
                detail,
                code,
                Instant.now()
        );
    }
}
