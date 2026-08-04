package com.jeepclub.backend.memberships.infra.mail;

import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DummyMemberActivationMailSender implements MemberActivationMailSender {

    @Value("${app.server.base-url}")
    private String baseUrl;

    @Override
    public void sendActivationLink(String recipientEmail, String recipientName, String activationToken) {
        String activationLink = baseUrl + "/membership-applications/activate?token=" + activationToken;

        log.info("=================================================");
        log.info("📧 MOCK EMAIL NOTIFICATION");
        log.info("To: {} <{}>", recipientName, recipientEmail);
        log.info("Subject: Bem-vindo ao Jeep Club — Ative seu acesso");
        log.info("Body: Olá, {}! Sua solicitação foi aprovada.", recipientName);
        log.info("Clique no link abaixo para ativar seu acesso:");
        log.info("{}", activationLink);
        log.info("Este link expira em 72 horas.");
        log.info("=================================================");
    }

    @Override
    public void sendRejectionNotice(String recipientEmail, String recipientName, String reason) {
        log.info("=================================================");
        log.info("📧 MOCK EMAIL NOTIFICATION");
        log.info("To: {} <{}>", recipientName, recipientEmail);
        log.info("Subject: Atualização sobre sua solicitação ao Jeep Club");
        log.info("Body: Olá, {}! Infelizmente sua solicitação foi rejeitada.", recipientName);
        log.info("Motivo: {}", reason != null && !reason.isBlank() ? reason : "Não informado.");
        log.info("=================================================");
    }
}
