package com.jeepclub.backend.memberships.infra.mail;

import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"dev", "test"})
public class DummyMemberActivationMailSender implements MemberActivationMailSender {

    @Override
    public void sendActivationLink(String recipientEmail, String recipientName, String activationToken) {
        log.info("Mock membership activation notification requested");
    }

    @Override
    public void sendRejectionNotice(String recipientEmail, String recipientName, String reason) {
        log.info("Mock membership rejection notification requested");
    }
}
