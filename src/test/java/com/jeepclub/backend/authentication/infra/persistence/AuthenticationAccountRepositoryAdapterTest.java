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
import jakarta.persistence.PersistenceException;
import com.jeepclub.backend.identity.api.module.UserStatus;
import com.jeepclub.backend.identity.infra.persistence.entity.UserEntity;
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
    void persistsAccountForExistingIdentityUsingTheSameSharedPrimaryKey() {
        UserEntity identity = persistIdentity();
        AuthenticationAccount created = repository.create(
                AuthenticationAccount.create(identity.getId(), "password-hash", CREATED_AT)
        );

        assertThat(created.getIdentityId()).isEqualTo(identity.getId());
        assertThat(repository.existsByIdentityId(identity.getId())).isTrue();
        assertThat(repository.findByIdentityId(identity.getId()))
                .hasValueSatisfying(found -> assertThat(found.getPasswordHash())
                        .isEqualTo("password-hash"));
        assertThat(repository.findByIdentityIdForUpdate(identity.getId()))
                .hasValueSatisfying(found -> assertThat(found.getIdentityId())
                        .isEqualTo(identity.getId()));

        AuthenticationAccountEntity entity = entityManager.find(
                AuthenticationAccountEntity.class,
                identity.getId()
        );
        assertThat(entity.getIdentityId()).isEqualTo(identity.getId());
        assertThat(entity.getUser().getId()).isEqualTo(identity.getId());
    }

    @Test
    void persistsAuthenticationAccessIndependently() {
        UserEntity identity = persistIdentity();
        AuthenticationAccount account = repository.create(
                AuthenticationAccount.create(identity.getId(), "password-hash", CREATED_AT)
        );
        account.disableAccess(CREATED_AT.plusSeconds(60));

        repository.save(account);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByIdentityId(identity.getId()))
                .hasValueSatisfying(found -> {
                    assertThat(found.getAccessStatus())
                            .isEqualTo(AuthenticationAccessStatus.DISABLED);
                    assertThat(found.getAccessDisabledAt())
                            .isEqualTo(CREATED_AT.plusSeconds(60));
                });
    }

    @Test
    void createDoesNotOverwriteExistingAccountWithSameIdentityId() {
        UserEntity identity = persistIdentity();
        repository.create(
                AuthenticationAccount.create(identity.getId(), "original-hash", CREATED_AT)
        );

        assertThatThrownBy(() -> repository.create(
                AuthenticationAccount.create(
                        identity.getId(),
                        "replacement-hash",
                        CREATED_AT.plusSeconds(1)
                )
        )).isInstanceOf(AuthenticationAccountConflictException.class);
    }

    @Test
    void rejectsAuthenticationAccountForNonexistentIdentity() {
        assertThatThrownBy(() -> repository.create(
                AuthenticationAccount.create(999_999L, "password-hash", CREATED_AT)
        )).isInstanceOf(PersistenceException.class);
    }

    @Test
    void doesNotCascadeIdentityDeletionThroughTheRelationship() {
        UserEntity identity = persistIdentity();
        repository.create(AuthenticationAccount.create(
                identity.getId(),
                "password-hash",
                CREATED_AT
        ));
        entityManager.flush();
        entityManager.clear();

        UserEntity managedIdentity = entityManager.find(UserEntity.class, identity.getId());
        entityManager.remove(managedIdentity);

        assertThatThrownBy(entityManager::flush)
                .isInstanceOf(PersistenceException.class);
    }

    private UserEntity persistIdentity() {
        UserEntity identity = new UserEntity();
        identity.setName("Persistence User");
        identity.setCpf("52998224725");
        identity.setEmail("persistence@example.com");
        identity.setStatus(UserStatus.ACTIVE);
        identity.setCreatedAt(CREATED_AT);
        entityManager.persist(identity);
        entityManager.flush();
        return identity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = AuthenticationAccountJpaRepository.class)
    @EntityScan(basePackageClasses = {AuthenticationAccountEntity.class, UserEntity.class})
    static class TestConfiguration {
    }
}
