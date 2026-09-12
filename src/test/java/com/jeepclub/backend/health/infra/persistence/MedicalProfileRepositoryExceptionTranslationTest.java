package com.jeepclub.backend.health.infra.persistence;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileConflictException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfilePersistenceException;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfilePersistenceUnavailableException;
import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.infra.persistence.adapter.MedicalProfileRepositoryAdapter;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileEntity;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileHistoryJpaRepository;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileJpaRepository;
import com.jeepclub.backend.health.infra.persistence.mapper.MedicalProfileHistoryMapper;
import com.jeepclub.backend.health.infra.persistence.mapper.MedicalProfileMapper;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalProfileRepositoryExceptionTranslationTest {

    private static final String INFRASTRUCTURE_DETAIL =
            "SQLSTATE 23000 constraint uk_medical_profile_owner";

    @Mock
    private MedicalProfileJpaRepository jpaRepository;
    @Mock
    private MedicalProfileHistoryJpaRepository historyRepository;
    @Mock
    private MedicalProfileMapper mapper;
    @Mock
    private MedicalProfileHistoryMapper historyMapper;

    private MedicalProfileRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MedicalProfileRepositoryAdapter(
                jpaRepository,
                historyRepository,
                mapper,
                historyMapper
        );
    }

    @Test
    void translatesOwnerUniqueConstraintToKnownFunctionalConflict() {
        MedicalProfile profile = profile();
        MedicalProfileEntity entity = new MedicalProfileEntity();
        ConstraintViolationException constraint = mock(ConstraintViolationException.class);
        when(constraint.getConstraintName()).thenReturn("uk_medical_profile_owner");
        when(mapper.toEntity(profile)).thenReturn(entity);
        when(jpaRepository.saveAndFlush(entity)).thenThrow(
                new DataIntegrityViolationException(INFRASTRUCTURE_DETAIL, constraint)
        );

        assertThatThrownBy(() -> adapter.save(profile))
                .isInstanceOf(MedicalProfileConflictException.class)
                .hasMessageNotContaining("constraint")
                .hasMessageNotContaining("SQLSTATE");
    }

    @Test
    void translatesLockFailureToConflictAndEmitsSafeObservation() {
        when(jpaRepository.findByOwnerForUpdate(
                MedicalProfileOwnerType.USER,
                7L
        )).thenThrow(new PessimisticLockingFailureException(INFRASTRUCTURE_DETAIL));
        Logger logger = (Logger) LoggerFactory.getLogger(
                MedicalProfileRepositoryAdapter.class
        );
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertThatThrownBy(() -> adapter.findByOwnerForUpdate(
                    MedicalProfileOwnerType.USER,
                    7L
            )).isInstanceOf(MedicalProfileConflictException.class)
                    .hasMessageNotContaining(INFRASTRUCTURE_DETAIL);

            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getFormattedMessage())
                        .contains("operation=find_by_owner_for_update")
                        .contains("reason=lock")
                        .doesNotContain(INFRASTRUCTURE_DETAIL)
                        .doesNotContain("SQLSTATE")
                        .doesNotContain("constraint");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void translatesTransientPersistenceFailureToUnavailable() {
        when(jpaRepository.findByOwnerTypeAndOwnerId(
                MedicalProfileOwnerType.USER,
                7L
        )).thenThrow(new QueryTimeoutException(INFRASTRUCTURE_DETAIL));

        assertThatThrownBy(() -> adapter.findByOwner(
                MedicalProfileOwnerType.USER,
                7L
        )).isInstanceOf(MedicalProfilePersistenceUnavailableException.class)
                .hasMessageNotContaining(INFRASTRUCTURE_DETAIL);
    }

    @Test
    void translatesUnexpectedPersistenceFailureToApplicationException() {
        when(jpaRepository.findById(1L)).thenThrow(
                new DataRetrievalFailureException(INFRASTRUCTURE_DETAIL)
        );

        assertThatThrownBy(() -> adapter.findById(1L))
                .isInstanceOf(MedicalProfilePersistenceException.class)
                .hasMessageNotContaining(INFRASTRUCTURE_DETAIL);
    }

    @Test
    void translatesNonOwnerIntegrityViolationToGenericPersistenceFailure() {
        MedicalProfile profile = profile();
        MedicalProfileEntity entity = new MedicalProfileEntity();
        when(mapper.toEntity(profile)).thenReturn(entity);
        when(jpaRepository.saveAndFlush(entity)).thenThrow(
                new DataIntegrityViolationException(INFRASTRUCTURE_DETAIL)
        );

        assertThatThrownBy(() -> adapter.save(profile))
                .isInstanceOf(MedicalProfilePersistenceException.class)
                .hasMessageNotContaining(INFRASTRUCTURE_DETAIL);
    }

    private MedicalProfile profile() {
        return MedicalProfile.create(
                MedicalProfileOwnerType.USER,
                7L,
                BloodType.O_POSITIVE,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.parse("2026-09-07T19:00:00Z")
        );
    }
}
