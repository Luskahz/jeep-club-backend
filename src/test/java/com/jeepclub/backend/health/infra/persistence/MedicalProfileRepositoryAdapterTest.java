package com.jeepclub.backend.health.infra.persistence;

import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.exception.MedicalProfileAlreadyDeletedException;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.infra.persistence.adapter.MedicalProfileRepositoryAdapter;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileEntity;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileHistoryEntity;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileHistoryJpaRepository;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileJpaRepository;
import com.jeepclub.backend.health.infra.persistence.mapper.MedicalProfileHistoryMapper;
import com.jeepclub.backend.health.infra.persistence.mapper.MedicalProfileMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = MedicalProfileRepositoryAdapterTest.JpaTestConfiguration.class)
@Import({
        MedicalProfileRepositoryAdapter.class,
        MedicalProfileMapper.class,
        MedicalProfileHistoryMapper.class
})
class MedicalProfileRepositoryAdapterTest {

    private static final Instant CREATED_AT =
            Instant.parse("2026-08-31T12:00:00Z");
    private static final Instant DELETED_AT =
            CREATED_AT.plusSeconds(300);

    @Autowired
    private MedicalProfileRepositoryAdapter repository;
    @Autowired
    private MedicalProfileJpaRepository medicalProfileJpaRepository;
    @Autowired
    private MedicalProfileHistoryJpaRepository historyJpaRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void deleteSavesHistoryAndRemovesOperationalEntity() {
        MedicalProfile saved = repository.save(profile());
        entityManager.flush();

        repository.delete(saved, 99L, DELETED_AT);
        entityManager.flush();
        entityManager.clear();

        assertThat(medicalProfileJpaRepository.findById(saved.getId())).isEmpty();
        assertThat(historyJpaRepository.findAll()).singleElement()
                .satisfies(history -> {
                    assertThat(history.getId()).isNotNull();
                    assertThat(history.getMedicalProfileId()).isEqualTo(saved.getId());
                    assertThat(history.getOwnerType()).isEqualTo(MedicalProfileOwnerType.USER);
                    assertThat(history.getOwnerId()).isEqualTo(7L);
                    assertThat(history.getDeletedByUserId()).isEqualTo(99L);
                    assertThat(history.getDeletedAt()).isEqualTo(DELETED_AT);
                    assertThat(history.getAllergies()).isEqualTo("Dipirona");
                });
    }

    @Test
    void historyProfileIdIsUnique() {
        historyJpaRepository.saveAndFlush(history(42L));

        assertThatThrownBy(() -> historyJpaRepository.saveAndFlush(
                history(42L)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteRejectsAnAlreadyDeletedMedicalProfile() {
        MedicalProfile saved = repository.save(profile());
        repository.delete(saved, 99L, DELETED_AT);
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> repository.delete(saved, 99L, DELETED_AT))
                .isInstanceOf(MedicalProfileAlreadyDeletedException.class);
    }

    @Test
    void ownerCanCreateANewProfileAfterOperationalDelete() {
        MedicalProfile saved = repository.save(profile());
        repository.delete(saved, 7L, DELETED_AT);
        entityManager.flush();

        MedicalProfile replacement = repository.save(profile());

        assertThat(replacement.getId()).isNotEqualTo(saved.getId());
        assertThat(replacement.getOwnerType()).isEqualTo(saved.getOwnerType());
        assertThat(replacement.getOwnerId()).isEqualTo(saved.getOwnerId());
    }

    private MedicalProfile profile() {
        return MedicalProfile.create(
                MedicalProfileOwnerType.USER,
                7L,
                BloodType.O_POSITIVE,
                "Dipirona",
                "Asma",
                "Bombinha",
                "Unimed",
                "Enfermaria",
                "123456789",
                "Maria",
                "12999999999",
                "Mãe",
                "Observação",
                CREATED_AT
        );
    }

    private MedicalProfileHistoryEntity history(Long profileId) {
        MedicalProfileHistoryEntity history = new MedicalProfileHistoryEntity();
        history.setMedicalProfileId(profileId);
        history.setOwnerType(MedicalProfileOwnerType.USER);
        history.setOwnerId(7L);
        history.setBloodType(BloodType.O_POSITIVE);
        history.setDeletedByUserId(99L);
        history.setCreatedAt(CREATED_AT);
        history.setUpdatedAt(CREATED_AT);
        history.setDeletedAt(DELETED_AT);
        return history;
    }

    @TestConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = MedicalProfileJpaRepository.class)
    @EntityScan(basePackageClasses = MedicalProfileEntity.class)
    static class JpaTestConfiguration {
    }
}
