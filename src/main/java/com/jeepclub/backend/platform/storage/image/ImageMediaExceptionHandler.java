package com.jeepclub.backend.platform.storage.image;

import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.exception.ApiExceptionHandler;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageFileException;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageKeyException;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = {
        "com.jeepclub.backend.platform.storage.image",
        "com.jeepclub.backend.iam.identity.api.http.controller",
        "com.jeepclub.backend.vehicles.api.http.controller",
        "com.jeepclub.backend.tools.api.http.controller",
        "com.jeepclub.backend.dependents.api.http.controller"
})
public class ImageMediaExceptionHandler extends ApiExceptionHandler {
    @ExceptionHandler(ImageUploadReadException.class)
    public ResponseEntity<ApiErrorResponse> readFailure(ImageUploadReadException exception) {
        return buildErrorResponse("IMAGE_UPLOAD_READ_FAILED", "Could not read the uploaded image.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(com.jeepclub.backend.shared.storage.exception.StorageOperationException.class)
    public ResponseEntity<ApiErrorResponse> unavailable(RuntimeException exception) {
        return buildErrorResponse("IMAGE_STORAGE_UNAVAILABLE", "Image storage is unavailable.", HttpStatus.SERVICE_UNAVAILABLE);
    }
    @ExceptionHandler({InvalidStorageFileException.class, InvalidStorageKeyException.class})
    public ResponseEntity<ApiErrorResponse> invalid(RuntimeException exception) {
        return buildErrorResponse("INVALID_IMAGE", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StorageObjectNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> missing(StorageObjectNotFoundException exception) {
        return buildErrorResponse("IMAGE_NOT_FOUND", exception.getMessage(), HttpStatus.NOT_FOUND);
    }
}
