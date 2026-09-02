package com.jeepclub.backend.identity.infra.integration.authentication;

import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.infra.integration.identity.LegacyIdentityQueryAdapter;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.UserJpaRepository;
import com.jeepclub.backend.identity.api.module.IdentityQuery;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(LegacyIdentityQueryAdapter.class)
class LegacyIdentityQueryAdapterTest {

    @Autowired
    private IdentityQuery identityQuery;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    void administrativeActivityDoesNotDependOnAuthenticationOrCredentialState() {
        UserEntity enabled = saveUser(
                "52998224725",
                AccountStatus.ACTIVE,
                AuthenticationStatus.ENABLED,
                CredentialStatus.PERMANENT
        );
        UserEntity locked = saveUser(
                "16899535009",
                AccountStatus.ACTIVE,
                AuthenticationStatus.LOCKED,
                CredentialStatus.PERMANENT
        );
        UserEntity pendingFirstAccess = saveUser(
                "11144477735",
                AccountStatus.ACTIVE,
                AuthenticationStatus.ENABLED,
                CredentialStatus.PENDING_FIRST_ACCESS
        );
        UserEntity changeRequired = saveUser(
                "12345678909",
                AccountStatus.ACTIVE,
                AuthenticationStatus.ENABLED,
                CredentialStatus.CHANGE_REQUIRED
        );
        UserEntity disabled = saveUser(
                "98765432100",
                AccountStatus.DISABLED,
                AuthenticationStatus.ENABLED,
                CredentialStatus.PERMANENT
        );

        assertThat(identityQuery.findAdministrativelyActiveIdentityIds())
                .containsExactly(
                        enabled.getId(),
                        locked.getId(),
                        pendingFirstAccess.getId(),
                        changeRequired.getId()
                );
        assertThat(identityQuery.isAdministrativelyActive(locked.getId())).isTrue();
        assertThat(identityQuery.isAdministrativelyActive(pendingFirstAccess.getId())).isTrue();
        assertThat(identityQuery.isAdministrativelyActive(changeRequired.getId())).isTrue();
        assertThat(identityQuery.isAdministrativelyActive(disabled.getId())).isFalse();
    }

    @Test
    void exposesLegacyIdentityExistenceWithoutLeakingAuthenticationState() {
        UserEntity identity = saveUser(
                "52998224725",
                AccountStatus.ACTIVE,
                AuthenticationStatus.LOCKED,
                CredentialStatus.PENDING_FIRST_ACCESS
        );

        assertThat(identityQuery.existsById(identity.getId())).isTrue();
        assertThat(identityQuery.existsByCpf(identity.getCpf())).isTrue();
        assertThat(identityQuery.existsByEmail(identity.getEmail())).isTrue();
        assertThat(identityQuery.existsById(identity.getId() + 1)).isFalse();
    }

    private UserEntity saveUser(
            String cpf,
            AccountStatus accountStatus,
            AuthenticationStatus authenticationStatus,
            CredentialStatus credentialStatus
    ) {
        UserEntity entity = new UserEntity();
        entity.setName("Identity " + cpf);
        entity.setEmail(cpf + "@example.com");
        entity.setCpf(cpf);
        entity.setRg("RG" + cpf);
        entity.setPasswordHash("hash");
        entity.setAccountStatus(accountStatus);
        entity.setAuthenticationStatus(authenticationStatus);
        entity.setCredentialStatus(credentialStatus);
        entity.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        entity.setFailedLoginAttempts(0);

        if (accountStatus == AccountStatus.DISABLED) {
            entity.setDisabledAt(Instant.parse("2026-01-02T00:00:00Z"));
        }

        return userJpaRepository.saveAndFlush(entity);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = UserJpaRepository.class)
    @EntityScan(basePackageClasses = UserEntity.class)
    static class TestConfiguration {
    }
}
