package com.jeepclub.backend.platform.mail;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** A configured SMTP host must never be silently ignored by dev/test dummy senders. */
@Component
@Profile("dev | test")
public class SimulatedMailConfigurationValidator {
    private final String host;

    public SimulatedMailConfigurationValidator(@Value("${spring.mail.host:}") String host) {
        this.host = host;
    }

    @PostConstruct
    void validate() {
        if (host != null && !host.isBlank()) {
            throw new IllegalStateException("spring.mail.host is configured but dev/test use simulated mail; use a production profile for SMTP");
        }
    }
}
