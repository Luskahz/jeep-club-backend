package com.jeepclub.backend.identity.infra.persistence;

import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.infra.persistence.adapter.UserRepositoryAdapter;
import com.jeepclub.backend.iam.identity.infra.persistence.jpa.*;
import com.jeepclub.backend.iam.identity.infra.persistence.mapper.UserMapper;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class UserRepositoryExceptionTranslationTest {
    @ParameterizedTest @MethodSource("unrelatedIntegrityFailures")
    void unknownDatabaseFailuresAreNotMisreportedAsUserUniquenessConflicts(DataIntegrityViolationException failure) {
        var jpa=mock(UserJpaRepository.class);
        var adapter=new UserRepositoryAdapter(mock(EntityManager.class),jpa,new UserMapper(),
                mock(UserProfileJpaRepository.class),mock(UserDeletionHistoryJpaRepository.class));
        when(jpa.saveAndFlush(any())).thenThrow(failure);
        var user=User.create("Synthetic",null,"test@example.com","52998224725",null,null,null,Instant.EPOCH);
        assertThatThrownBy(() -> adapter.create(user)).isSameAs(failure);
    }
    static Stream<Arguments> unrelatedIntegrityFailures() {
        return Stream.of(
                Arguments.of(new DataIntegrityViolationException("Synthetic unclassified integrity failure")),
                Arguments.of(new DataIntegrityViolationException("Synthetic unnamed constraint",
                        new ConstraintViolationException("Synthetic",new SQLException("Synthetic"),null))),
                Arguments.of(new DataIntegrityViolationException("Synthetic unrelated constraint",
                        new ConstraintViolationException("Synthetic",new SQLException("Synthetic"),"fk_unrelated"))));
    }
}
