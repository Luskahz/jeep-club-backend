package com.jeepclub.backend.dependents.core.application.result;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record DependentResult(
        Long id,
        String name,
        String cpf,
        LocalDate birthDate,
        RelationshipType relationshipType,
        String phoneNumber,
        Long userId,
        DependentStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static DependentResult from(Dependent dependent) {
        Objects.requireNonNull(
                dependent,
                "Dependent cannot be null"
        );

        return new DependentResult(
                dependent.getId(),
                dependent.getName(),
                dependent.getCpf(),
                dependent.getBirthDate(),
                dependent.getRelationshipType(),
                dependent.getPhoneNumber(),
                dependent.getUserId(),
                dependent.getStatus(),
                dependent.getCreatedAt(),
                dependent.getUpdatedAt()
        );
    }
}
