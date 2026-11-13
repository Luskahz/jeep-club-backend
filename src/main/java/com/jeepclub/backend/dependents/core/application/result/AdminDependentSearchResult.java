package com.jeepclub.backend.dependents.core.application.result;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record AdminDependentSearchResult(
        Long id,
        String name,
        String cpf,
        LocalDate birthDate,
        RelationshipType relationshipType,
        String phoneNumber,
        Long userId,
        String ownerName,
        DependentStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt,
        String photoUrl
) {

    @Override
    public String toString() {
        return "AdminDependentSearchResult[id=" + id + ", privateData=[REDACTED]]";
    }

    public AdminDependentSearchResult withOwnerName(String resolvedOwnerName) {
        return new AdminDependentSearchResult(
                id,
                name,
                cpf,
                birthDate,
                relationshipType,
                phoneNumber,
                userId,
                resolvedOwnerName,
                status,
                createdAt,
                updatedAt,
                archivedAt,
                photoUrl
        );
    }

    public static AdminDependentSearchResult from(Dependent dependent, String ownerName) {
        Objects.requireNonNull(dependent, "Dependent cannot be null");
        String photoUrl = dependent.getPhotoStorageKey() == null
                ? null
                : "/users/" + dependent.getUserId() + "/dependents/" + dependent.getId() + "/photo";
        return new AdminDependentSearchResult(
                dependent.getId(),
                dependent.getName(),
                dependent.getCpf(),
                dependent.getBirthDate(),
                dependent.getRelationshipType(),
                dependent.getPhoneNumber(),
                dependent.getUserId(),
                ownerName,
                dependent.getStatus(),
                dependent.getCreatedAt(),
                dependent.getUpdatedAt(),
                dependent.getArchivedAt(),
                photoUrl
        );
    }
}
