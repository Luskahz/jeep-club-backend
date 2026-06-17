package com.jeepclub.backend.authentication.core.domain.exception.user;

public class UserNotDisabledException extends RuntimeException {

    public UserNotDisabledException(Long userId) {
        super(
                "User with id " + userId
                        + " is not disabled."
        );
    }
}