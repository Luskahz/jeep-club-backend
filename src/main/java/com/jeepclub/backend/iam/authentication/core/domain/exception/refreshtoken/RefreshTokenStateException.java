package com.jeepclub.backend.iam.authentication.core.domain.exception.refreshtoken;

public class RefreshTokenStateException
        extends RuntimeException {

    public RefreshTokenStateException(
            String message
    ) {
        super(message);
    }
}