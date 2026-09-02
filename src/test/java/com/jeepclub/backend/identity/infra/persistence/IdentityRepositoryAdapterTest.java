package com.jeepclub.backend.identity.infra.persistence;

import com.jeepclub.backend.identity.core.application.exception.IdentityConflictException;
import com.jeepclub.backend.identity.core.domain.model.Identity;
import com.jeepclub.backend.identity.core.repository.IdentityRepository;
import com.jeepclub.backend.identity.infra.persistence.adapter.IdentityRepositoryAdapter;
import com.jeepclub.backend.identity.infra.persistence.entity.IdentityEntity;
import com.jeepclub.backend.identity.infra.persistence.jpa.IdentityJpaRepository;
import com.jeepclub.backend.identity.infra.persistence.mapper.IdentityMapper;
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
@Import({IdentityRepositoryAdapter.class, IdentityMapper.class})
class IdentityRepositoryAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private IdentityRepository repository;

    @Test
    void persistsAndQueriesIdentityByAdministrativeStatus() {
        Identity first = repository.create(identity(
                "First Identity",
                "529.982.247-25",
                "first@example.com",
                "12.345.678-9"
        ));
        Identity second = repository.create(identity(
                "Second Identity",
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
                "First Identity",
                "52998224725",
                "first@example.com",
                "123456789"
        ));

        assertThatThrownBy(() -> repository.create(identity(
                "Second Identity",
                "529.982.247-25",
                "second@example.com",
                "987654321"
        ))).isInstanceOf(IdentityConflictException.class);
    }

    @Test
    void translatesEmailUniqueConstraintViolation() {
        repository.create(identity(
                "First Identity",
                "52998224725",
                "SAME@example.com",
                "123456789"
        ));

        assertThatThrownBy(() -> repository.create(identity(
                "Second Identity",
                "16899535009",
                "same@EXAMPLE.com",
                "987654321"
        ))).isInstanceOf(IdentityConflictException.class);
    }

    @Test
    void translatesRgUniqueConstraintViolation() {
        repository.create(identity(
                "First Identity",
                "52998224725",
                "first@example.com",
                "12.345.678-9"
        ));

        assertThatThrownBy(() -> repository.create(identity(
                "Second Identity",
                "16899535009",
                "second@example.com",
                "123456789"
        ))).isInstanceOf(IdentityConflictException.class);
    }

    private Identity identity(
            String name,
            String cpf,
            String email,
            String rg
    ) {
        return Identity.create(
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
    @EnableJpaRepositories(basePackageClasses = IdentityJpaRepository.class)
    @EntityScan(basePackageClasses = IdentityEntity.class)
    static class TestConfiguration {
    }
}
