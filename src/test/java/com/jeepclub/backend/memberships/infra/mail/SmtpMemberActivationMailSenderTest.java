package com.jeepclub.backend.memberships.infra.mail;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpMemberActivationMailSenderTest {
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"   "})
    void absentReasonUsesOnlyTheApprovedGenericMessage(String reason) {
        assertThat(SmtpMemberActivationMailSender.rejectionBody("Candidate", reason)).isEqualTo("""
                Olá, Candidate.

                Informamos que sua solicitação de associação ao Jeep Club Tamoios não foi aprovada.

                Em caso de dúvidas, entre em contato com a administração do clube.""");
    }
    @Test void smtpReceivesTheApprovedContentAndOnlyThePersistedReason() {
        JavaMailSender provider = mock(JavaMailSender.class);
        var sender = new SmtpMemberActivationMailSender(provider);
        org.springframework.test.util.ReflectionTestUtils.setField(sender, "from", "club@example.com");
        sender.sendRejectionNotice("candidate@example.com", "Candidate", "  Persisted reason  ");
        var capture = ArgumentCaptor.forClass(SimpleMailMessage.class); verify(provider).send(capture.capture());
        assertThat(capture.getValue().getText()).contains("Motivo: Persisted reason.")
                .startsWith("Olá, Candidate.").endsWith("Em caso de dúvidas, entre em contato com a administração do clube.");
        assertThat(capture.getValue().getTo()).containsExactly("candidate@example.com");
    }
}
