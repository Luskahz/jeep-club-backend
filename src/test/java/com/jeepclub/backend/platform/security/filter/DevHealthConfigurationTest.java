package com.jeepclub.backend.platform.security.filter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;

class DevHealthConfigurationTest {
    private ApplicationContextRunner runner(String profile) {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=" + profile);
    }

    @Test void devDoesNotRequireAnIntentionallyUnconfiguredSmtpServer() {
        runner("dev").run(context -> {
            assertFalse(context.getEnvironment().getProperty("management.health.mail.enabled", Boolean.class, true));
            assertFalse(context.getEnvironment().getProperty("springdoc.api-docs.enabled", Boolean.class, true));
            assertFalse(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled", Boolean.class, true));
        });
    }

    @Test void otherProfilesKeepTheDefaultMailHealthCheck() {
        runner("prod").run(context ->
                assertTrue(context.getEnvironment().getProperty("management.health.mail.enabled", Boolean.class, true)));
    }
}
