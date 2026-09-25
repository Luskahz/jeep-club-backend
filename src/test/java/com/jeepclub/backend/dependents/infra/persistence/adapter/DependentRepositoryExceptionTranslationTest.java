package com.jeepclub.backend.dependents.infra.persistence.adapter;

import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentHistoryJpaRepository;
import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentJpaRepository;
import com.jeepclub.backend.dependents.infra.persistence.mapper.DependentHistoryMapper;
import com.jeepclub.backend.dependents.infra.persistence.mapper.DependentMapper;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DependentRepositoryExceptionTranslationTest {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"unrelated_constraint", "no_hibernate_cause"})
    void unrelatedIntegrityFailuresAreNotMisreportedAsCpfConflicts(String constraint) {
        DependentJpaRepository jpa = mock(DependentJpaRepository.class);
        DependentRepositoryAdapter repository = new DependentRepositoryAdapter(jpa,
                mock(DependentHistoryJpaRepository.class), mock(DependentMapper.class),
                mock(DependentHistoryMapper.class));
        Throwable cause = "no_hibernate_cause".equals(constraint)
                ? new SQLException("unrelated")
                : new ConstraintViolationException("unrelated", new SQLException(), constraint);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("integrity", cause);
        when(jpa.saveAndFlush(any())).thenThrow(failure);
        assertThatThrownBy(() -> repository.save(mock(Dependent.class))).isSameAs(failure);
    }
}
