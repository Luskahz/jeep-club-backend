package com.jeepclub.backend.platform.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionMailConfigurationValidatorTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ProductionMailConfigurationValidator.class)
            .withPropertyValues("spring.profiles.active=prod");

    private final String[] configuredMail = {
            "spring.mail.host=smtp.example.com", "spring.mail.port=587",
            "spring.mail.username=user", "spring.mail.password=secret",
            "app.mail.from=no-reply@example.com",
            "app.user-facing.password-reset-url=https://app.example.com/reset",
            "app.user-facing.membership-activation-url=https://app.example.com/activate"
    };

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
        runner.withPropertyValues(configuredMail)
                .withPropertyValues("app.user-facing.account-activation-url=https://app.example.com/account/activate")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @ParameterizedTest
    @CsvSource({
            "http://app.example.com/activate,false,true",
            "https://app.example.com/activate,false,true",
            "http://app.example.com/activate,true,false",
            "https://app.example.com/activate,true,true",
            "javascript:alert(1),false,false",
            "'https://bad host/activate',false,false",
            "http:///activate,false,false"
    })
    void accountActivationUrlHonorsStructureAndHttpsSwitch(String url, boolean requireHttps, boolean accepted) {
        runner.withPropertyValues(configuredMail)
                .withPropertyValues("app.user-facing.account-activation-url=" + url,
                        "app.user-facing.account-activation-require-https=" + requireHttps)
                .run(context -> assertThat(context.getStartupFailure() == null).isEqualTo(accepted));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"dev", "test"})
    void simulatedProfilesRejectConfiguredSmtpInsteadOfSilentlyUsingDummy(String profile) {
        new ApplicationContextRunner().withUserConfiguration(SimulatedMailConfigurationValidator.class)
                .withPropertyValues("spring.profiles.active=" + profile, "spring.mail.host=smtp.example.com")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("simulated mail");
                });
    }
}
