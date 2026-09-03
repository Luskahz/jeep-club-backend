package com.jeepclub.backend.dependents.system;

import com.jeepclub.backend.identity.core.domain.enums.IdentityStatus;
import com.jeepclub.backend.identity.infra.persistence.entity.IdentityEntity;
import com.jeepclub.backend.identity.infra.persistence.jpa.IdentityJpaRepository;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentHistoryEntity;
import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentHistoryJpaRepository;
import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DependentSystemFlowTest {

    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    @Autowired
    private DependentService dependentService;
    @Autowired
    private IdentityJpaRepository identityJpaRepository;
    @Autowired
    private DependentJpaRepository dependentJpaRepository;
    @Autowired
    private DependentHistoryJpaRepository historyJpaRepository;

    private IdentityEntity user;

    @BeforeEach
    void setUp() {
        historyJpaRepository.deleteAll();
        dependentJpaRepository.deleteAll();
        identityJpaRepository.deleteAll();
        user = identityJpaRepository.saveAndFlush(user());
    }

    @Test
    void createDisableEnableUpdateAndDeleteDependent() {
        DependentResult created = dependentService.create(
                "Pedro Silva", "529.982.247-25", LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, "11988887777", user.getId()
        );

        assertThat(dependentService.disable(created.id(), user.getId()).status())
                .isEqualTo(DependentStatus.DISABLED);
        assertThat(dependentService.enable(created.id(), user.getId()).status())
                .isEqualTo(DependentStatus.ACTIVE);

        DependentResult updated = dependentService.update(
                created.id(), "Pedro Ramos", "111.444.777-35",
                LocalDate.of(2010, 5, 20), RelationshipType.CHILD,
                null, user.getId()
        );
        assertThat(updated.name()).isEqualTo("Pedro Ramos");

        dependentService.delete(created.id(), user.getId());

        assertThat(dependentJpaRepository.findById(created.id())).isEmpty();
        assertThat(historyJpaRepository.findAll()).singleElement()
                .extracting(DependentHistoryEntity::getDependentId)
                .isEqualTo(created.id());
    }

    @Test
    void failureWhileSavingHistoryRollsBackOperationalDelete() {
        DependentResult created = dependentService.create(
                "Pedro Silva", "529.982.247-25", LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, null, user.getId()
        );
        historyJpaRepository.saveAndFlush(history(created));

        assertThatThrownBy(() -> dependentService.delete(created.id(), user.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(dependentJpaRepository.findById(created.id())).isPresent();
    }

    private DependentHistoryEntity history(DependentResult dependent) {
        DependentHistoryEntity history = new DependentHistoryEntity();
        history.setDependentId(dependent.id());
        history.setName(dependent.name());
        history.setCpf(dependent.cpf());
        history.setBirthDate(dependent.birthDate());
        history.setRelationshipType(dependent.relationshipType());
        history.setPhoneNumber(dependent.phoneNumber());
        history.setUserId(dependent.userId());
        history.setStatus(dependent.status());
        history.setDeletedByUserId(user.getId());
        history.setCreatedAt(dependent.createdAt());
        history.setUpdatedAt(dependent.updatedAt());
        history.setDeletedAt(NOW.plusSeconds(60));
        return history;
    }

    private IdentityEntity user() {
        IdentityEntity entity = new IdentityEntity();
        entity.setName("Titular Fluxo");
        entity.setCpf("39053344705");
        entity.setStatus(IdentityStatus.ACTIVE);
        entity.setCreatedAt(NOW);
        return entity;
    }
}
