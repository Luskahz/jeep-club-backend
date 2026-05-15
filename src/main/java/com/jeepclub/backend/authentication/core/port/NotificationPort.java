package com.jeepclub.backend.authentication.core.port;

public interface NotificationPort {
    void sendPasswordResetLink(String email, String resetLink);
}
