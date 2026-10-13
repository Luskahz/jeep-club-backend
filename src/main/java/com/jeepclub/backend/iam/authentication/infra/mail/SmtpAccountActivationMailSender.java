package com.jeepclub.backend.iam.authentication.infra.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("!dev & !test")
@RequiredArgsConstructor
public class SmtpAccountActivationMailSender implements AccountActivationMailSender {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;
    @Value("${app.user-facing.account-activation-url}")
    private String activationUrl;

    @Override
    public void send(String email, String name, String token) {
        String link = UriComponentsBuilder.fromUriString(activationUrl)
                .queryParam("token", token).build().encode().toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Jeep Club - ative sua conta");
        message.setText("Olá, " + name + ". Defina sua senha neste link:\n\n" + link);
        mailSender.send(message);
    }
}
