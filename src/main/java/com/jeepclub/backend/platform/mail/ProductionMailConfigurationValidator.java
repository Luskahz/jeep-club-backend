package com.jeepclub.backend.platform.mail;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev & !test")
public class ProductionMailConfigurationValidator {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String from;
    private final String passwordResetUrl;
    private final String membershipActivationUrl;

    public ProductionMailConfigurationValidator(
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.port:0}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${app.mail.from:}") String from,
            @Value("${app.user-facing.password-reset-url:}") String passwordResetUrl,
            @Value("${app.user-facing.membership-activation-url:}") String membershipActivationUrl
    ) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.from = from;
        this.passwordResetUrl = passwordResetUrl;
        this.membershipActivationUrl = membershipActivationUrl;
    }

    @PostConstruct
    void validate() {
        requireText(host, "spring.mail.host");
        if (port <= 0) {
            throw new IllegalStateException("spring.mail.port must be positive");
        }
        requireText(username, "spring.mail.username");
        requireText(password, "spring.mail.password");
        requireText(from, "app.mail.from");
        requireExternalUrl(passwordResetUrl, "app.user-facing.password-reset-url");
        requireExternalUrl(membershipActivationUrl, "app.user-facing.membership-activation-url");
    }

    private static void requireText(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " must be configured outside dev/test");
        }
    }

    private static void requireExternalUrl(String value, String property) {
        requireText(value, property);
        if (value.contains("localhost") || value.contains("127.0.0.1")) {
            throw new IllegalStateException(property + " must be explicitly configured outside dev/test");
        }
    }
}
