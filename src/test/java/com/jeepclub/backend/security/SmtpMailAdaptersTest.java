package com.jeepclub.backend.security;

import com.jeepclub.backend.iam.authentication.infra.adapter.SmtpEmailNotificationAdapter;
import com.jeepclub.backend.memberships.infra.mail.SmtpMemberActivationMailSender;
import com.jeepclub.backend.iam.authentication.infra.mail.SmtpAccountActivationMailSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpMailAdaptersTest {

    @Test
    void eligiblePasswordRecoveryIsDeliveredThroughJavaMailSender() {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        SmtpEmailNotificationAdapter adapter = new SmtpEmailNotificationAdapter(javaMailSender);
        ReflectionTestUtils.setField(adapter, "from", "no-reply@example.com");

        adapter.sendPasswordResetLink("member@example.com", "https://app.example.com/reset?token=secret");

        SimpleMailMessage message = capturedMessage(javaMailSender);
        assertThat(message.getFrom()).isEqualTo("no-reply@example.com");
        assertThat(message.getTo()).containsExactly("member@example.com");
        assertThat(message.getText()).contains("https://app.example.com/reset?token=secret");
    }

    @Test
    void membershipActivationDeliversExactlyThePreparedFrontendLink() {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        SmtpMemberActivationMailSender adapter = new SmtpMemberActivationMailSender(
                javaMailSender
        );
        ReflectionTestUtils.setField(adapter, "from", "no-reply@example.com");

        adapter.sendActivationLink("member@example.com", "Member", "https://app.example.com/membership/activate?token=raw-token");

        SimpleMailMessage message = capturedMessage(javaMailSender);
        assertThat(message.getFrom()).isEqualTo("no-reply@example.com");
        assertThat(message.getTo()).containsExactly("member@example.com");
        assertThat(message.getText())
                .contains("https://app.example.com/membership/activate?token=raw-token");
    }

    @Test
    void administrativeInvitationUsesProductionJavaMailSender() {
        JavaMailSender javaMailSender = mock(JavaMailSender.class);
        SmtpAccountActivationMailSender adapter = new SmtpAccountActivationMailSender(javaMailSender);
        ReflectionTestUtils.setField(adapter, "from", "no-reply@example.com");
        ReflectionTestUtils.setField(adapter, "activationUrl", "https://app.example.com/account/activate");

        adapter.send("member@example.com", "Member", "raw-token");

        SimpleMailMessage message = capturedMessage(javaMailSender);
        assertThat(message.getFrom()).isEqualTo("no-reply@example.com");
        assertThat(message.getTo()).containsExactly("member@example.com");
        assertThat(message.getText()).contains("https://app.example.com/account/activate?token=raw-token");
    }

    private SimpleMailMessage capturedMessage(JavaMailSender javaMailSender) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        return captor.getValue();
    }
}
