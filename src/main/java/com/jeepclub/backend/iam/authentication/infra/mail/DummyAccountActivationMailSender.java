package com.jeepclub.backend.iam.authentication.infra.mail;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class DummyAccountActivationMailSender implements AccountActivationMailSender {
    @Override
    public void send(String email, String name, String token) {
        // Deliberately does not log the recipient or token.
    }
}
