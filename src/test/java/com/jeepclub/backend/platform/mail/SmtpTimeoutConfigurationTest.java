package com.jeepclub.backend.platform.mail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpTimeoutConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MailSenderAutoConfiguration.class)
            .withInitializer(context -> {
                try {
                    context.getEnvironment().getPropertySources().addLast(
                            new ResourcePropertySource("classpath:application.properties"));
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            })
            .withPropertyValues("SPRING_MAIL_HOST=localhost");

    @Test
    void defaultsApplyToActualJavaMailSender() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var properties = context.getBean(JavaMailSenderImpl.class).getJavaMailProperties();
            assertThat(properties.getProperty("mail.smtp.connectiontimeout")).isEqualTo("5000");
            assertThat(properties.getProperty("mail.smtp.timeout")).isEqualTo("10000");
            assertThat(properties.getProperty("mail.smtp.writetimeout")).isEqualTo("10000");
        });
    }

    @Test
    void environmentOverridesReachActualJavaMailSender() {
        runner.withPropertyValues("SPRING_MAIL_SMTP_CONNECTION_TIMEOUT=1200",
                "SPRING_MAIL_SMTP_READ_TIMEOUT=2300", "SPRING_MAIL_SMTP_WRITE_TIMEOUT=3400")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var properties = context.getBean(JavaMailSenderImpl.class).getJavaMailProperties();
                    assertThat(properties.getProperty("mail.smtp.connectiontimeout")).isEqualTo("1200");
                    assertThat(properties.getProperty("mail.smtp.timeout")).isEqualTo("2300");
                    assertThat(properties.getProperty("mail.smtp.writetimeout")).isEqualTo("3400");
                });
    }
}
