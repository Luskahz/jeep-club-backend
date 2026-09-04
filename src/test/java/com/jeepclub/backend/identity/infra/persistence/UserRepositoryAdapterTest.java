package com.jeepclub.backend.identity.infra.persistence;

import com.jeepclub.backend.identity.core.application.exception.UserConflictException;
import com.jeepclub.backend.identity.core.domain.model.User;
import com.jeepclub.backend.identity.core.repository.UserRepository;
import com.jeepclub.backend.identity.infra.persistence.adapter.UserRepositoryAdapter;
import com.jeepclub.backend.identity.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.identity.infra.persistence.jpa.UserJpaRepository;
import com.jeepclub.backend.identity.infra.persistence.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({UserRepositoryAdapter.class, UserMapper.class})
class UserRepositoryAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private UserRepository repository;

    @Test
    void persistsAndQueriesIdentityByAdministrativeStatus() {
        User first = repository.create(identity(
                "First User",
                "529.982.247-25",
                "first@example.com",
                "12.345.678-9"
        ));
        User second = repository.create(identity(
                "Second User",
                "168.995.350-09",
                "second@example.com",
                "98.765.432-1"
        ));

        second.disable(CREATED_AT.plusSeconds(60));
        repository.save(second);

        assertThat(first.getId()).isPositive();
        assertThat(repository.findById(first.getId()))
                .hasValueSatisfying(found -> assertThat(found.getCpf())
                        .isEqualTo(first.getCpf()));
        assertThat(repository.findByIdForUpdate(first.getId()))
                .hasValueSatisfying(found -> assertThat(found.getId())
                        .isEqualTo(first.getId()));
        assertThat(repository.existsById(first.getId())).isTrue();
        assertThat(repository.existsByCpf("52998224725")).isTrue();
        assertThat(repository.existsByEmail("first@example.com")).isTrue();
        assertThat(repository.existsByRg("123456789")).isTrue();
        assertThat(repository.existsActiveById(first.getId())).isTrue();
        assertThat(repository.existsActiveById(second.getId())).isFalse();
        assertThat(repository.findActiveIds()).containsExactly(first.getId());
    }

    @Test
    void translatesCpfUniqueConstraintViolation() {
        repository.create(identity(
                "First User",
                "52998224725",
                "first@example.com",
                "123456789"
        ));

        assertThatThrownBy(() -> repository.create(identity(
                "Second User",
                "529.982.247-25",
                "second@example.com",
                "987654321"
        ))).isInstanceOf(UserConflictException.class);
    }

    @Test
    void translatesEmailUniqueConstraintViolation() {
        repository.create(identity(
                "First User",
                "52998224725",
                "SAME@example.com",
                "123456789"
        ));

        assertThatThrownBy(() -> repository.create(identity(
                "Second User",
                "16899535009",
                "same@EXAMPLE.com",
                "987654321"
        ))).isInstanceOf(UserConflictException.class);
    }

    @Test
    void translatesRgUniqueConstraintViolation() {
        repository.create(identity(
                "First User",
                "52998224725",
                "first@example.com",
                "12.345.678-9"
        ));

        assertThatThrownBy(() -> repository.create(identity(
                "Second User",
                "16899535009",
                "second@example.com",
                "123456789"
        ))).isInstanceOf(UserConflictException.class);
    }

    private User identity(
            String name,
            String cpf,
            String email,
            String rg
    ) {
        return User.create(
                name,
                null,
                email,
                cpf,
                rg,
                "+55 (12) 99999-9999",
                null,
                CREATED_AT
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = UserJpaRepository.class)
    @EntityScan(basePackageClasses = UserEntity.class)
    static class TestConfiguration {
    }
}
