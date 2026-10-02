package com.jeepclub.backend.platform.mail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionMailConfigurationValidatorTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ProductionMailConfigurationValidator.class)
            .withPropertyValues("spring.profiles.active=prod");

    @Test
    void productionFailsExplicitlyWhenSmtpConfigurationIsMissing() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                    .hasMessageContaining("spring.mail.host");
        });
    }

    @Test
    void productionAcceptsExplicitSmtpAndUserFacingUrls() {
        runner.withPropertyValues(
                "spring.mail.host=smtp.example.com",
                "spring.mail.port=587",
                "spring.mail.username=user",
                "spring.mail.password=secret",
                "app.mail.from=no-reply@example.com",
                "app.user-facing.password-reset-url=https://app.example.com/reset",
                "app.user-facing.membership-activation-url=https://app.example.com/activate"
        ).run(context -> assertThat(context).hasNotFailed());
    }
}
