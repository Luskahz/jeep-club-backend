package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.enums.ChargeAssignmentType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChargeAssignment {

    private Long id;
    private Long chargeDefinitionId;
    private ChargeAssignmentType assignmentType;
    private Long targetId;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    private ChargeAssignment(
            Long id,
            Long chargeDefinitionId,
            ChargeAssignmentType assignmentType,
            Long targetId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.chargeDefinitionId = validateId(chargeDefinitionId, "chargeDefinitionId");
        this.assignmentType = Objects.requireNonNull(assignmentType, "assignmentType cannot be null");
        this.targetId = targetId;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = updatedAt;

        validateTargetConsistency(this.assignmentType, this.targetId);
    }

    public static ChargeAssignment assignToAllMembers(
            Long chargeDefinitionId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        return new ChargeAssignment(
                null,
                chargeDefinitionId,
                ChargeAssignmentType.ALL_MEMBERS,
                null,
                true,
                now,
                null
        );
    }

    public static ChargeAssignment assignToUser(
            Long chargeDefinitionId,
            Long userId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        return new ChargeAssignment(
                null,
                chargeDefinitionId,
                ChargeAssignmentType.USER,
                userId,
                true,
                now,
                null
        );
    }

    public static ChargeAssignment assignToRole(
            Long chargeDefinitionId,
            Long roleId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        return new ChargeAssignment(
                null,
                chargeDefinitionId,
                ChargeAssignmentType.ROLE,
                roleId,
                true,
                now,
                null
        );
    }

    public static ChargeAssignment reconstitute(
            Long id,
            Long chargeDefinitionId,
            ChargeAssignmentType assignmentType,
            Long targetId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ChargeAssignment(
                id,
                chargeDefinitionId,
                assignmentType,
                targetId,
                active,
                createdAt,
                updatedAt
        );
    }

    public void activate(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (active) {
            throw new IllegalStateException("Charge assignment is already active.");
        }

        this.active = true;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (!active) {
            throw new IllegalStateException("Charge assignment is already inactive.");
        }

        this.active = false;
        this.updatedAt = now;
    }

    public boolean appliesToAllMembers() {
        return assignmentType == ChargeAssignmentType.ALL_MEMBERS;
    }

    public boolean appliesToUser() {
        return assignmentType == ChargeAssignmentType.USER;
    }

    public boolean appliesToRole() {
        return assignmentType == ChargeAssignmentType.ROLE;
    }

    private static Long validateId(Long id, String fieldName) {
        Objects.requireNonNull(id, fieldName + " cannot be null");

        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }

        return id;
    }

    private static void validateTargetConsistency(
            ChargeAssignmentType assignmentType,
            Long targetId
    ) {
        if (assignmentType == ChargeAssignmentType.ALL_MEMBERS) {
            if (targetId != null) {
                throw new IllegalArgumentException("targetId must be null when assignmentType is ALL_MEMBERS.");
            }

            return;
        }

        validateId(targetId, "targetId");
    }
}