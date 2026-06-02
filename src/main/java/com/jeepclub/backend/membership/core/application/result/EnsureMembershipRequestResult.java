package com.jeepclub.backend.membership.core.application.result;

import com.jeepclub.backend.membership.core.domain.model.MembershipApplication;

import java.util.Objects;

public record EnsureMembershipRequestResult(
        MembershipApplication application,
        boolean created
) {

    public EnsureMembershipRequestResult {
        Objects.requireNonNull(application, "application cannot be null");
    }

    public static EnsureMembershipRequestResult created(MembershipApplication application) {
        return new EnsureMembershipRequestResult(application, true);
    }

    public static EnsureMembershipRequestResult existing(MembershipApplication application) {
        return new EnsureMembershipRequestResult(application, false);
    }
}