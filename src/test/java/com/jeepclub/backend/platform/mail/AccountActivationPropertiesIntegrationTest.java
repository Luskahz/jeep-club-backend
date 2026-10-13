package com.jeepclub.backend.platform.mail;

import com.jeepclub.backend.iam.authentication.infra.mail.AccountActivationMailSender;
import com.jeepclub.backend.iam.authentication.infra.mail.DummyAccountActivationMailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "APP_ACCOUNT_ACTIVATION_URL=https://app.example.com/account/activate",
        "APP_ACCOUNT_ACTIVATION_TTL=PT6H",
        "APP_ACCOUNT_ACTIVATION_REQUIRE_HTTPS=true"
})
@ActiveProfiles("test")
class AccountActivationPropertiesIntegrationTest {
    @Autowired private Environment environment;
    @Autowired private AccountActivationMailSender mailSender;

    @Test
    void springResolvesAllThreeDeploymentVariables() {
        assertThat(environment.getProperty("app.user-facing.account-activation-url"))
                .isEqualTo("https://app.example.com/account/activate");
        assertThat(environment.getProperty("app.authentication.account-activation-ttl"))
                .isEqualTo("PT6H");
        assertThat(environment.getProperty("app.user-facing.account-activation-require-https"))
                .isEqualTo("true");
    }

    @Test
    void testProfileUsesSenderThatDoesNotTransmitMail() {
        assertThat(mailSender).isInstanceOf(DummyAccountActivationMailSender.class);
        mailSender.send("recipient@example.com", "Recipient", "secret-token");
    }
}
