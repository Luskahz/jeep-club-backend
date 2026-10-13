package com.jeepclub.backend.iam.authentication.infra.mail;

public interface AccountActivationMailSender {
    void send(String email, String name, String token);
}
