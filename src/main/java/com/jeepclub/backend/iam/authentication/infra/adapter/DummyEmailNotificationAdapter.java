package com.jeepclub.backend.iam.authentication.infra.adapter;

import com.jeepclub.backend.iam.authentication.core.port.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DummyEmailNotificationAdapter implements NotificationPort {

    @Override
    public void sendPasswordResetLink(String email, String resetLink) {
        log.info("=================================================");
        log.info("📧 MOCK EMAIL NOTIFICATION");
        log.info("To: {}", email);
        log.info("Subject: Recuperação de Senha - Jeep Club");
        log.info("Body: Você solicitou a recuperação da sua senha.");
        log.info("Por favor, acesse o link abaixo para criar uma nova senha:");
        log.info("{}", resetLink);
        log.info("=================================================");
    }
}
