package com.jeepclub.backend.platform.export;
import com.jeepclub.backend.shared.export.ExportException;
import com.jeepclub.backend.platform.web.exception.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
@RestControllerAdvice
public class ExportExceptionHandler extends ApiExceptionHandler {
    @ExceptionHandler(ExportException.class)
    public ResponseEntity<ApiErrorResponse> handle(ExportException e) {
        HttpStatus status = switch (e.reason()) {
            case INVALID_FILTER -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case LIMIT -> HttpStatus.PAYLOAD_TOO_LARGE;
            case GENERATION -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return buildErrorResponse("EXPORT_" + e.reason(), "Não foi possível gerar a exportação.", status);
    }
}
