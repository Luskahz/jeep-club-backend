package com.jeepclub.backend.memberships.core.port;

public interface MemberActivationMailSender {

    void sendActivationLink(String recipientEmail, String recipientName, String activationLink);

    void sendRejectionNotice(String recipientEmail, String recipientName, String reason);
}
