package com.jeepclub.backend.authentication.core.application.exceptions.refreshtoken;

public class RFNotFoundException extends RuntimeException {

    public RFNotFoundException(Long refreshTokenId) {
        super(
                "Refresh token not found with id: "
                        + refreshTokenId
        );
    }
    public RFNotFoundException(String message) {
        super(message);
    }
}