package com.jeepclub.backend.authentication.core.domain.exception.user;

public class UserInvalidPasswordException extends RuntimeException {

    public UserInvalidPasswordException(Long userId) {
        super("Invalid password for user id: " + userId);
    }
}
