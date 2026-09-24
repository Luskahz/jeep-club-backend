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

@RestControllerAdvice(basePackages = {
        "com.jeepclub.backend.platform.storage.image",
        "com.jeepclub.backend.iam.identity.api.http.controller",
        "com.jeepclub.backend.vehicles.api.http.controller",
        "com.jeepclub.backend.tools.api.http.controller"
})
public class ImageMediaExceptionHandler extends ApiExceptionHandler {
    @ExceptionHandler({InvalidStorageFileException.class, InvalidStorageKeyException.class})
    public ResponseEntity<ApiErrorResponse> invalid(RuntimeException exception) {
        return buildErrorResponse("INVALID_IMAGE", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StorageObjectNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> missing(StorageObjectNotFoundException exception) {
        return buildErrorResponse("IMAGE_NOT_FOUND", exception.getMessage(), HttpStatus.NOT_FOUND);
    }
}
