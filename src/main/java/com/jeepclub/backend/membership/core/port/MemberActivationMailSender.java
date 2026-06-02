package com.jeepclub.backend.membership.core.port;

public interface MemberActivationMailSender {

    void sendActivationLink(String recipientEmail, String recipientName, String activationToken);

    void sendRejectionNotice(String recipientEmail, String recipientName, String reason);
}