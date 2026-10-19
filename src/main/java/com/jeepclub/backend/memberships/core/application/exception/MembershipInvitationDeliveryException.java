package com.jeepclub.backend.memberships.core.application.exception;

/** Provider failures may include the complete mail message and its bearer URL. */
public class MembershipInvitationDeliveryException extends RuntimeException {
    public MembershipInvitationDeliveryException() {
        super("Membership invitation delivery failed; retry the operation.");
    }
}
