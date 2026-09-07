package com.jeepclub.backend.health.infra.persistence;

import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditEvent;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOperation;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOutcome;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.port.MedicalProfileAuditTrail;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileAuditEntity;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileAuditJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MedicalProfileAuditPersistenceIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-07T17:00:00Z");

    @Autowired
    private MedicalProfileAuditTrail auditTrail;
    @Autowired
    private MedicalProfileAuditJpaRepository repository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    @AfterEach
    void cleanAuditEvents() {
        repository.deleteAll();
    }

    @Test
    void successfulEventIsPersistedOnlyAfterBusinessTransactionCommits() {
        transactionTemplate.executeWithoutResult(status -> {
            auditTrail.record(event(MedicalProfileAuditOutcome.SUCCEEDED));
            assertThat(repository.count()).isZero();
        });

        assertThat(repository.findAll()).singleElement().satisfies(entity -> {
            assertThat(entity.getActorUserId()).isEqualTo(99L);
            assertThat(entity.getOwnerType()).isEqualTo(MedicalProfileOwnerType.USER);
            assertThat(entity.getOwnerId()).isEqualTo(7L);
            assertThat(entity.getOperation()).isEqualTo(MedicalProfileAuditOperation.READ);
            assertThat(entity.getOccurredAt()).isEqualTo(NOW);
        });
    }

    @Test
    void deniedEventSurvivesCallerTransactionRollback() {
        transactionTemplate.executeWithoutResult(status -> {
            auditTrail.record(event(MedicalProfileAuditOutcome.DENIED));
            status.setRollbackOnly();
        });

        assertThat(repository.findAll()).singleElement().satisfies(entity ->
                assertThat(entity.getOutcome())
                        .isEqualTo(MedicalProfileAuditOutcome.DENIED)
        );
    }

    @Test
    void persistenceEntityHasNoClinicalContentColumns() {
        Set<String> fields = Stream.of(MedicalProfileAuditEntity.class.getDeclaredFields())
                .map(field -> field.getName())
                .collect(Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder(
                "id",
                "actorUserId",
                "ownerType",
                "ownerId",
                "operation",
                "outcome",
                "occurredAt"
        );
    }

    private MedicalProfileAuditEvent event(MedicalProfileAuditOutcome outcome) {
        return new MedicalProfileAuditEvent(
                99L,
                MedicalProfileOwnerType.USER,
                7L,
                MedicalProfileAuditOperation.READ,
                outcome,
                NOW
        );
    }
}
