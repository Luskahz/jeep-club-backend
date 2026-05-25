package com.jeepclub.backend.membership.core.domain.model;

import com.jeepclub.backend.membership.core.domain.enums.MembershipApplicationStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MembershipApplication {

    private Long id;
    private String name;
    private String cpf;
    private String email;
    private String phoneNumber;
    private String message;
    private MembershipApplicationStatus status;
    private String rejectionReason;
    private Instant requestedAt;
    private Instant updatedAt;

    public static MembershipApplication create(
            String name,
            String cpf,
            String email,
            String phoneNumber,
            String message,
            Instant now
    ) {
        MembershipApplication app = new MembershipApplication();
        app.name = name;
        app.cpf = cpf;
        app.email = email;
        app.phoneNumber = phoneNumber;
        app.message = message;
        app.status = MembershipApplicationStatus.PENDING;
        app.requestedAt = now;
        app.updatedAt = now;
        return app;
    }

    public static MembershipApplication reconstitute(
            Long id,
            String name,
            String cpf,
            String email,
            String phoneNumber,
            String message,
            MembershipApplicationStatus status,
            String rejectionReason,
            Instant requestedAt,
            Instant updatedAt
    ) {
        MembershipApplication app = new MembershipApplication();
        app.id = id;
        app.name = name;
        app.cpf = cpf;
        app.email = email;
        app.phoneNumber = phoneNumber;
        app.message = message;
        app.status = status;
        app.rejectionReason = rejectionReason;
        app.requestedAt = requestedAt;
        app.updatedAt = updatedAt;
        return app;
    }

    public void markAsInviteSent(Instant now) {
        this.status = MembershipApplicationStatus.INVITE_SENT;
        this.updatedAt = now;
    }

    public void markAsRejected(Instant now) {
        this.status = MembershipApplicationStatus.REJECTED;
        this.updatedAt = now;
    }
}