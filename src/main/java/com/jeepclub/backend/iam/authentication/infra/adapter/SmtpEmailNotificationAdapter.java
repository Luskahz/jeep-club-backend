package com.jeepclub.backend.iam.authentication.infra.adapter;

import com.jeepclub.backend.iam.authentication.core.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev & !test")
@RequiredArgsConstructor
public class SmtpEmailNotificationAdapter implements NotificationPort {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void sendPasswordResetLink(String email, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Recuperação de senha - Jeep Club");
        message.setText("Use o link a seguir para definir uma nova senha:\n\n" + resetLink);
        mailSender.send(message);
    }
}
