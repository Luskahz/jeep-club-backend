package com.jeepclub.backend.memberships.infra.mail;

import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import com.jeepclub.backend.memberships.core.port.MembershipActivationUrlProperties;
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
public class SmtpMemberActivationMailSender implements MemberActivationMailSender {

    private final JavaMailSender mailSender;
    private final MembershipActivationUrlProperties urlProperties;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void sendActivationLink(String recipientEmail, String recipientName, String activationToken) {
        String link = UriComponentsBuilder.fromUriString(urlProperties.activationUrl())
                .queryParam("token", activationToken)
                .build()
                .encode()
                .toUriString();
        send(
                recipientEmail,
                "Bem-vindo ao Jeep Club - ative seu acesso",
                "Olá, " + recipientName + "! Defina sua senha pelo link:\n\n" + link
        );
    }

    @Override
    public void sendRejectionNotice(String recipientEmail, String recipientName, String reason) {
        send(
                recipientEmail,
                "Atualização sobre sua solicitação ao Jeep Club",
                "Olá, " + recipientName + ". Sua solicitação foi rejeitada.\n\nMotivo: " + reason
        );
    }

    private void send(String recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
