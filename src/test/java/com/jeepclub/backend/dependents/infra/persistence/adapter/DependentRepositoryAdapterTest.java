package com.jeepclub.backend.dependents.infra.persistence.adapter;

import com.jeepclub.backend.dependents.core.application.exception.DependentCpfAlreadyInUseException;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentEntity;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentHistoryEntity;
import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentHistoryJpaRepository;
import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentJpaRepository;
import com.jeepclub.backend.dependents.infra.persistence.mapper.DependentHistoryMapper;
import com.jeepclub.backend.dependents.infra.persistence.mapper.DependentMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        DependentRepositoryAdapter.class,
        DependentMapper.class,
        DependentHistoryMapper.class
})
class DependentRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    @Autowired
    private DependentRepositoryAdapter repository;
    @Autowired
    private DependentJpaRepository dependentJpaRepository;
    @Autowired
    private DependentHistoryJpaRepository historyJpaRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void savesFindsAndKeepsCpfReservedForDisabledDependent() {
        Dependent saved = repository.save(dependent(DependentStatus.DISABLED));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(saved.getId())).get()
                .extracting(Dependent::getStatus)
                .isEqualTo(DependentStatus.DISABLED);
        assertThat(repository.existsByCpf("12345678900")).isTrue();
    }

    @Test
    void deleteSavesHistoryAndRemovesOperationalEntity() {
        Dependent saved = repository.save(dependent(DependentStatus.ACTIVE));
        entityManager.flush();

        repository.delete(saved, 99L, NOW.plusSeconds(60));
        entityManager.flush();
        entityManager.clear();

        assertThat(dependentJpaRepository.findById(saved.getId())).isEmpty();
        assertThat(historyJpaRepository.findAll()).singleElement()
                .satisfies(history -> {
                    assertThat(history.getDependentId()).isEqualTo(saved.getId());
                    assertThat(history.getDeletedByUserId()).isEqualTo(99L);
                    assertThat(history.getDeletedAt()).isEqualTo(NOW.plusSeconds(60));
                    assertThat(history.getStatus()).isEqualTo(DependentStatus.ACTIVE);
                });
    }

    @Test
    void historyDependentIdIsUnique() {
        historyJpaRepository.saveAndFlush(history(42L));

        assertThatThrownBy(() -> historyJpaRepository.saveAndFlush(history(42L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void translatesOperationalCpfUniqueConstraintViolation() {
        repository.save(dependent(DependentStatus.ACTIVE));

        assertThatThrownBy(() -> repository.save(dependent(DependentStatus.DISABLED)))
                .isInstanceOf(DependentCpfAlreadyInUseException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    private Dependent dependent(DependentStatus status) {
        Dependent dependent = Dependent.create(
                "Pedro Silva", "12345678900", LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, "11988887777", 1L, NOW
        );
        if (status == DependentStatus.DISABLED) {
            dependent.disable(NOW.plusSeconds(30));
        }
        return dependent;
    }

    private DependentHistoryEntity history(Long dependentId) {
        DependentHistoryEntity history = new DependentHistoryEntity();
        history.setDependentId(dependentId);
        history.setName("Pedro Silva");
        history.setCpf("12345678900");
        history.setBirthDate(LocalDate.of(2010, 5, 20));
        history.setRelationshipType(RelationshipType.CHILD);
        history.setUserId(1L);
        history.setStatus(DependentStatus.ACTIVE);
        history.setDeletedByUserId(99L);
        history.setCreatedAt(NOW);
        history.setDeletedAt(NOW.plusSeconds(60));
        return history;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = DependentJpaRepository.class)
    @EntityScan(basePackageClasses = DependentEntity.class)
    static class TestConfiguration {
    }
}
