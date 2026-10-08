package com.jeepclub.backend.health.infra.persistence;

import com.jeepclub.backend.health.core.application.exceptions.MedicalProfilePersistenceUnavailableException;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.infra.persistence.adapter.MedicalProfileRepositoryAdapter;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileJpaRepository;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileHistoryJpaRepository;
import com.jeepclub.backend.health.infra.persistence.mapper.MedicalProfileMapper;
import com.jeepclub.backend.health.infra.persistence.mapper.MedicalProfileHistoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = MedicalProfileRepositoryAdapterTest.JpaTestConfiguration.class)
@Import({MedicalProfileRepositoryAdapter.class, MedicalProfileMapper.class, MedicalProfileHistoryMapper.class})
class MedicalProfileDeleteRollbackTest {
    @Autowired MedicalProfileRepositoryAdapter repository;
    @Autowired MedicalProfileHistoryJpaRepository history;
    @Autowired TransactionTemplate transactions;
    @MockitoSpyBean MedicalProfileJpaRepository jpa;

    @Test
    void failureAfterHistoryFlushRollsBackBothHistoryAndOperationalDeletion() {
        Instant at = Instant.parse("2026-09-20T12:00:00Z");
        var saved = transactions.execute(s -> repository.save(MedicalProfile.create(MedicalProfileOwnerType.USER,
                81001L, null, null, null, null, null, null, null, null, null, null, null, at)));
        try {
            doThrow(new QueryTimeoutException("SYNTHETIC_FAILURE")).when(jpa).flush();
            assertThatThrownBy(() -> transactions.executeWithoutResult(s -> repository.delete(saved, 1L, at.plusSeconds(1))))
                    .isInstanceOf(MedicalProfilePersistenceUnavailableException.class);
            reset(jpa);
            transactions.executeWithoutResult(s -> {
                assertThat(jpa.findById(saved.getId())).isPresent();
                assertThat(history.findAll()).noneMatch(row -> row.getMedicalProfileId().equals(saved.getId()));
            });
        } finally {
            reset(jpa);
            transactions.executeWithoutResult(s -> jpa.deleteById(saved.getId()));
        }
    }
}
