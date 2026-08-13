package com.jeepclub.backend.memberships.core.application.exception;

public class MembershipApplicantBlockedException extends RuntimeException {

    public MembershipApplicantBlockedException() {
        super("Não é possível realizar uma nova solicitação de associação.");
    }
}
