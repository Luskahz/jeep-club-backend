package com.jeepclub.backend.membership.infra.mail;

import com.jeepclub.backend.membership.core.port.MemberActivationMailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
// essa implementação de mandar mensagem pro email do cara funciona?
// validar e fazer funcionar.
public class DummyMemberActivationMailSender implements MemberActivationMailSender {

    @Override
    public void sendActivationLink(String recipientEmail, String recipientName, String activationToken) {
        log.info("[MEMBERSHIP - DUMMY] Enviando link de ativação para: {} ({}) | Token: {}",
                recipientEmail, recipientName, activationToken);
    }

    @Override
    public void sendRejectionNotice(String recipientEmail, String recipientName, String reason) {
        log.info("[MEMBERSHIP - DUMMY] Enviando rejeição para: {} ({}) | Motivo: {}",
                recipientEmail, recipientName, reason);
    }
}