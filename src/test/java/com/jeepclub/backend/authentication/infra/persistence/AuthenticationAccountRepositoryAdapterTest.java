package com.jeepclub.backend.authentication.infra.persistence;

import com.jeepclub.backend.authentication.core.application.exceptions.account.AuthenticationAccountConflictException;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationAccessStatus;
import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.authentication.infra.persistence.adapter.AuthenticationAccountRepositoryAdapter;
import com.jeepclub.backend.authentication.infra.persistence.entity.AuthenticationAccountEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.AuthenticationAccountJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.AuthenticationAccountMapper;
import jakarta.persistence.EntityManager;
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
@Import({AuthenticationAccountRepositoryAdapter.class, AuthenticationAccountMapper.class})
class AuthenticationAccountRepositoryAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private AuthenticationAccountRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void preservesIdentityIdAsAssignedPrimaryKey() {
        AuthenticationAccount created = repository.create(
                AuthenticationAccount.create(42L, "password-hash", CREATED_AT)
        );

        assertThat(created.getIdentityId()).isEqualTo(42L);
        assertThat(repository.existsByIdentityId(42L)).isTrue();
        assertThat(repository.findByIdentityId(42L))
                .hasValueSatisfying(found -> assertThat(found.getPasswordHash())
                        .isEqualTo("password-hash"));
        assertThat(repository.findByIdentityIdForUpdate(42L))
                .hasValueSatisfying(found -> assertThat(found.getIdentityId())
                        .isEqualTo(42L));
    }

    @Test
    void persistsAuthenticationAccessIndependently() {
        AuthenticationAccount account = repository.create(
                AuthenticationAccount.create(42L, "password-hash", CREATED_AT)
        );
        account.disableAccess(CREATED_AT.plusSeconds(60));

        repository.save(account);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByIdentityId(42L))
                .hasValueSatisfying(found -> {
                    assertThat(found.getAccessStatus())
                            .isEqualTo(AuthenticationAccessStatus.DISABLED);
                    assertThat(found.getAccessDisabledAt())
                            .isEqualTo(CREATED_AT.plusSeconds(60));
                });
    }

    @Test
    void createDoesNotOverwriteExistingAccountWithSameIdentityId() {
        repository.create(
                AuthenticationAccount.create(42L, "original-hash", CREATED_AT)
        );

        assertThatThrownBy(() -> repository.create(
                AuthenticationAccount.create(
                        42L,
                        "replacement-hash",
                        CREATED_AT.plusSeconds(1)
                )
        )).isInstanceOf(AuthenticationAccountConflictException.class);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = AuthenticationAccountJpaRepository.class)
    @EntityScan(basePackageClasses = AuthenticationAccountEntity.class)
    static class TestConfiguration {
    }
}
