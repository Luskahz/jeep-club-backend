package com.jeepclub.backend.iam.authentication.infra.adapter;

import com.jeepclub.backend.iam.authentication.core.port.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

@Slf4j
@Component
@Profile({"dev", "test"})
public class DummyEmailNotificationAdapter implements NotificationPort {

    @Override
    public void sendPasswordResetLink(String email, String resetLink) {
        log.info("Mock password recovery notification requested");
    }
}
