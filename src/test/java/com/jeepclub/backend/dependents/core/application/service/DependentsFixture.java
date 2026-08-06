package com.jeepclub.backend.dependents.core.application.service;

import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;

import java.time.Instant;
import java.time.LocalDate;

final class DependentsFixture {

    private static final Instant CREATED_AT = Instant.parse("2026-06-30T12:00:00Z");

    private DependentsFixture() {
    }

    static Dependent dependent(Long id, Long socioId) {
        return Dependent.reconstitute(
                id,
                "Pedro Silva",
                "12345678900",
                LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD,
                "11988887777",
                true,
                CREATED_AT,
                socioId,
                CREATED_AT,
                CREATED_AT
        );
    }
}
