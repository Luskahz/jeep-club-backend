package com.jeepclub.backend.billing.infra.persistence.mapper;

import com.jeepclub.backend.billing.core.domain.model.assignment.AllMembersChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.ChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.EventParticipantsChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.RoleChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.UserChargeAssignment;
import com.jeepclub.backend.billing.infra.persistence.entity.assignment.AllMembersChargeAssignmentEntity;
import com.jeepclub.backend.billing.infra.persistence.entity.assignment.ChargeAssignmentEntity;
import com.jeepclub.backend.billing.infra.persistence.entity.assignment.EventParticipantsChargeAssignmentEntity;
import com.jeepclub.backend.billing.infra.persistence.entity.assignment.RoleChargeAssignmentEntity;
import com.jeepclub.backend.billing.infra.persistence.entity.assignment.UserChargeAssignmentEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ChargeAssignmentMapper {

    public ChargeAssignment toDomain(ChargeAssignmentEntity entity) {
        Objects.requireNonNull(entity, "ChargeAssignmentEntity cannot be null");

        if (entity instanceof AllMembersChargeAssignmentEntity assignment) {
            return AllMembersChargeAssignment.reconstitute(
                    assignment.getId(),
                    assignment.getChargeDefinitionId(),
                    assignment.isActive(),
                    assignment.getCreatedAt(),
                    assignment.getUpdatedAt()
            );
        }

        if (entity instanceof UserChargeAssignmentEntity assignment) {
            return UserChargeAssignment.reconstitute(
                    assignment.getId(),
                    assignment.getChargeDefinitionId(),
                    assignment.getUserId(),
                    assignment.isActive(),
                    assignment.getCreatedAt(),
                    assignment.getUpdatedAt()
            );
        }

        if (entity instanceof RoleChargeAssignmentEntity assignment) {
            return RoleChargeAssignment.reconstitute(
                    assignment.getId(),
                    assignment.getChargeDefinitionId(),
                    assignment.getRoleId(),
                    assignment.isActive(),
                    assignment.getCreatedAt(),
                    assignment.getUpdatedAt()
            );
        }

        if (entity instanceof EventParticipantsChargeAssignmentEntity assignment) {
            return EventParticipantsChargeAssignment.reconstitute(
                    assignment.getId(),
                    assignment.getChargeDefinitionId(),
                    assignment.getEventId(),
                    assignment.isActive(),
                    assignment.getCreatedAt(),
                    assignment.getUpdatedAt()
            );
        }

        throw unsupportedEntityType(entity);
    }

    public ChargeAssignmentEntity toEntity(ChargeAssignment chargeAssignment) {
        Objects.requireNonNull(chargeAssignment, "ChargeAssignment cannot be null");

        if (chargeAssignment instanceof AllMembersChargeAssignment assignment) {
            return new AllMembersChargeAssignmentEntity(
                    assignment.getId(),
                    assignment.getChargeDefinitionId(),
                    assignment.isActive(),
                    assignment.getCreatedAt(),
                    assignment.getUpdatedAt()
            );
        }

        if (chargeAssignment instanceof UserChargeAssignment assignment) {
            return new UserChargeAssignmentEntity(
                    assignment.getId(),
                    assignment.getChargeDefinitionId(),
                    assignment.getUserId(),
                    assignment.isActive(),
                    assignment.getCreatedAt(),
                    assignment.getUpdatedAt()
            );
        }

        if (chargeAssignment instanceof RoleChargeAssignment assignment) {
            return new RoleChargeAssignmentEntity(
                    assignment.getId(),
                    assignment.getChargeDefinitionId(),
                    assignment.getRoleId(),
                    assignment.isActive(),
                    assignment.getCreatedAt(),
                    assignment.getUpdatedAt()
            );
        }

        if (chargeAssignment instanceof EventParticipantsChargeAssignment assignment) {
            return new EventParticipantsChargeAssignmentEntity(
                    assignment.getId(),
                    assignment.getChargeDefinitionId(),
                    assignment.getEventId(),
                    assignment.isActive(),
                    assignment.getCreatedAt(),
                    assignment.getUpdatedAt()
            );
        }

        throw unsupportedDomainType(chargeAssignment);
    }

    private static IllegalArgumentException unsupportedEntityType(ChargeAssignmentEntity entity) {
        return new IllegalArgumentException(
                "Unsupported charge assignment entity type: " + entity.getClass().getName()
        );
    }

    private static IllegalArgumentException unsupportedDomainType(ChargeAssignment chargeAssignment) {
        return new IllegalArgumentException(
                "Unsupported charge assignment domain type: " + chargeAssignment.getClass().getName()
        );
    }
}