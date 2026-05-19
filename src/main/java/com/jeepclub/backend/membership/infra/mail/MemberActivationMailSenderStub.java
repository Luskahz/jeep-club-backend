package com.jeepclub.backend.membership.infra.mail;

import com.jeepclub.backend.membership.core.port.MemberActivationMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MemberActivationMailSenderStub implements MemberActivationMailSender {

    private static final Logger log = LoggerFactory.getLogger(MemberActivationMailSenderStub.class);

    @Override
    public void sendActivationLink(String recipientEmail, String recipientName, String activationToken) {
        log.info("[STUB] Enviando link de ativação para {} <{}> — token: {}", recipientName, recipientEmail, activationToken);
    }

    @Override
    public void sendRejectionNotice(String recipientEmail, String recipientName, String reason) {
        log.info("[STUB] Enviando rejeição para {} <{}> — motivo: {}", recipientName, recipientEmail, reason);
    }
}