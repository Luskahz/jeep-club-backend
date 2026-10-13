package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.AuthenticationAccountJpaRepository;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.platform.logging.SystemLogServiceAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class AdminUserAuditFailureIntegrationTest {
    @Autowired private UserRegistration registration;
    @Autowired private UserQuery users;
    @Autowired private AuthenticationAccountJpaRepository accounts;
    @MockitoBean private SystemLogServiceAdapter audit;

    @Test
    void auditFailureRollsBackUserAndAccountTogether() {
        doThrow(new IllegalStateException("audit unavailable")).when(audit).recordInTransaction(any());
        String cpf = "64320987514";
        assertThatThrownBy(() -> registration.createAdministratively(
                new UserRegistrationData("Audit target", null, null, cpf, null, null, null, Instant.now()),
                UserActivationMethod.TEMPORARY_PASSWORD, 17L))
                .isInstanceOf(IllegalStateException.class);
        assertThat(users.existsByCpf(cpf)).isFalse();
        assertThat(accounts.count()).isZero();
    }
}
