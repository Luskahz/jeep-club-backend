package com.jeepclub.backend.authentication.core.application.exceptions.user;

public class UserIdNotFoundException extends RuntimeException {

    public UserIdNotFoundException(String message) {
        super(message);
    }

    public UserIdNotFoundException(Long userId) {
        super(
                "User not found with id: "
                        + userId
        );
    }
}