package com.jeepclub.backend.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import com.jeepclub.backend.iam.authentication.infra.adapter.DummyEmailNotificationAdapter;
import com.jeepclub.backend.iam.authentication.infra.adapter.SmtpEmailNotificationAdapter;
import com.jeepclub.backend.memberships.infra.mail.DummyMemberActivationMailSender;
import com.jeepclub.backend.memberships.infra.mail.SmtpMemberActivationMailSender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class MailSecretLoggingTest {

    @Test
    void dummyAdaptersAreRestrictedToControlledProfiles() {
        assertThat(DummyEmailNotificationAdapter.class.getAnnotation(Profile.class).value())
                .containsExactly("dev", "test");
        assertThat(DummyMemberActivationMailSender.class.getAnnotation(Profile.class).value())
                .containsExactly("dev", "test");
        assertThat(SmtpEmailNotificationAdapter.class.getAnnotation(Profile.class).value())
                .containsExactly("!dev & !test");
        assertThat(SmtpMemberActivationMailSender.class.getAnnotation(Profile.class).value())
                .containsExactly("!dev & !test");
    }

    @Test
    void passwordRecoveryDummyDoesNotLogResetLinkOrToken() {
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = capture(
                DummyEmailNotificationAdapter.class
        );
        new DummyEmailNotificationAdapter().sendPasswordResetLink(
                "user@example.com", "https://client/reset?token=raw-secret"
        );

        assertThat(messages(appender)).doesNotContain("raw-secret", "token=", "https://client");
    }

    @Test
    void activationDummyDoesNotLogActivationLinkOrToken() {
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = capture(
                DummyMemberActivationMailSender.class
        );
        new DummyMemberActivationMailSender().sendActivationLink(
                "user@example.com", "User", "raw-secret"
        );

        assertThat(messages(appender)).doesNotContain("raw-secret", "token=", "http");
    }

    private ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> capture(Class<?> type) {
        Logger logger = (Logger) LoggerFactory.getLogger(type);
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private String messages(ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender) {
        return appender.list.stream()
                .map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
    }
}
