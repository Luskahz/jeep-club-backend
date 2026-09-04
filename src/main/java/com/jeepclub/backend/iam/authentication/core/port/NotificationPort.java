package com.jeepclub.backend.iam.authentication.core.port;

public interface NotificationPort {
    void sendPasswordResetLink(String email, String resetLink);
}
