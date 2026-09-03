package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.api.module.user.UserQuery;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserField;
import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.authentication.core.application.service.account.AuthenticationAccountProvisioningService;
import com.jeepclub.backend.authentication.core.application.service.user.AdminUserService;
import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationAccessStatus;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.identity.api.module.IdentityQuery;
import com.jeepclub.backend.identity.api.module.IdentityRegistrationData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IdentityAuthenticationCutoverIntegrationTest {

    @Autowired private AuthenticationAccountProvisioningService provisioningService;
    @Autowired private AuthenticationAccountRepository accountRepository;
    @Autowired private IdentityQuery identityQuery;
    @Autowired private UserQuery userQuery;
    @Autowired private AdminUserService adminUserService;

    @Test
    void administrationCombinesBothAggregatesAndDisablesBothAtomically() {
        Long identityId = provision("39053344705", "cutover@example.com");

        AdminUserResult listed = adminUserService.findAll(
                new AdminUserFilter(identityId, null, null, null, null, null,
                        null, null, null, null, null, null, null, null),
                EnumSet.allOf(AdminUserField.class),
                PageRequest.of(0, 10, Sort.by("updatedAt"))
        ).getContent().get(0);

        assertThat(listed.id()).isEqualTo(identityId);
        assertThat(listed.email()).isEqualTo("cutover@example.com");
        assertThat(listed.accountStatus()).isEqualTo(AccountStatus.ACTIVE);

        AdminUserResult disabled = adminUserService.disable(identityId);

        assertThat(disabled.accountStatus()).isEqualTo(AccountStatus.DISABLED);
        assertThat(identityQuery.isAdministrativelyActive(identityId)).isFalse();
        assertThat(accountRepository.findByIdentityId(identityId).orElseThrow().getAccessStatus())
                .isEqualTo(AuthenticationAccessStatus.DISABLED);
    }

    @Test
    void pendingFirstAccessIdentityRemainsAdministrativelyActive() {
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        Long identityId = provisioningService.provisionPendingFirstAccess(
                new IdentityRegistrationData("Pending", null, null, "11144477735",
                        null, null, null, now),
                "hash"
        );

        assertThat(identityQuery.isAdministrativelyActive(identityId)).isTrue();
        assertThat(userQuery.existsActiveUserById(identityId)).isTrue();
        assertThat(userQuery.findActiveUserIds()).contains(identityId);
    }

    private Long provision(String cpf, String email) {
        return provisioningService.provision(
                new IdentityRegistrationData("Cutover User", null, email, cpf,
                        null, "5511999999999", null,
                        Instant.parse("2026-08-01T12:00:00Z")),
                "hash"
        );
    }
}
