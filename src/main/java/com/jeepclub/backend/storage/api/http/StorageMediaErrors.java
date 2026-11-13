package com.jeepclub.backend.storage.api.http;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;

@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes={StorageMediaController.class,StorageAdminController.class})
public class StorageMediaErrors {
    @ExceptionHandler(com.jeepclub.backend.storage.core.repository.UploadAdmission.LimitExceeded.class) public ResponseEntity<ProblemDetail> limited() {return error(429,"Upload allowance exceeded");}
    @ExceptionHandler({AccessDeniedException.class,com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException.class}) public ResponseEntity<ProblemDetail> denied() { return error(404,"Media unavailable"); }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<ProblemDetail> invalid() { return error(400,"Invalid media request"); }
    @ExceptionHandler({IllegalStateException.class,java.io.IOException.class,java.io.UncheckedIOException.class,com.jeepclub.backend.shared.storage.exception.StorageException.class}) public ResponseEntity<ProblemDetail> failed() { return error(503,"Media operation unavailable"); }
    private ResponseEntity<ProblemDetail> error(int status,String detail) { var problem=ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status),detail);if(status==404) problem.setProperty("reasonCode","CONTENT_UNAVAILABLE");return ResponseEntity.status(status).header(HttpHeaders.CACHE_CONTROL,"private, no-store").body(problem); }
}
