package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.core.application.service.account.AuthenticationAccountProvisioningService;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationAccessStatus;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.identity.api.module.UserQuery;
import com.jeepclub.backend.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.identity.api.module.UserStatus;
import com.jeepclub.backend.identity.core.application.query.user.AdminUserField;
import com.jeepclub.backend.identity.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.identity.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.identity.core.application.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
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
    @Autowired private UserQuery identityQuery;
    @Autowired private AdminUserService adminUserService;

    @Test
    void administrationCombinesBothAggregatesAndDisablesBothAtomically() {
        Long identityId = provision("39053344705", "cutover@example.com");

        AdminUserResult listed = adminUserService.findAll(
                new AdminUserFilter(identityId, null, null, null, null, null,
                        null, null, null, null, null, null, null),
                EnumSet.allOf(AdminUserField.class),
                PageRequest.of(0, 10, Sort.by("updatedAt"))
        ).getContent().get(0);

        assertThat(listed.id()).isEqualTo(identityId);
        assertThat(listed.email()).isEqualTo("cutover@example.com");
        assertThat(listed.status()).isEqualTo(UserStatus.ACTIVE);

        AdminUserResult disabled = adminUserService.disable(identityId);

        assertThat(disabled.status()).isEqualTo(UserStatus.DISABLED);
        assertThat(identityQuery.isAdministrativelyActive(identityId)).isFalse();
        assertThat(accountRepository.findByIdentityId(identityId).orElseThrow().getAccessStatus())
                .isEqualTo(AuthenticationAccessStatus.DISABLED);
    }

    @Test
    void pendingFirstAccessIdentityRemainsAdministrativelyActive() {
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        Long identityId = provisioningService.provisionPendingFirstAccess(
                new UserRegistrationData("Pending", null, null, "11144477735",
                        null, null, null, now),
                "hash"
        );

        assertThat(identityQuery.isAdministrativelyActive(identityId)).isTrue();
        assertThat(identityQuery.isAdministrativelyActive(identityId)).isTrue();
        assertThat(identityQuery.findAdministrativelyActiveUserIds()).contains(identityId);
    }

    @Test
    void administrativeReadModelKeepsDatabasePaginationSearchSortAndSparseFields() {
        provision("Read Model Alpha", "12345678901", "alpha-read-model@example.com");
        provision("Read Model Beta", "12345678902", "beta-read-model@example.com");

        Page<AdminUserResult> page = adminUserService.findAll(
                new AdminUserFilter(null, null, null, null, null, null,
                        null, null, null, null, null, null, "read model"),
                EnumSet.of(AdminUserField.ID, AdminUserField.NAME),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "name"))
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).singleElement().satisfies(result -> {
            assertThat(result.name()).isEqualTo("Read Model Beta");
            assertThat(result.id()).isNotNull();
            assertThat(result.email()).isNull();
            assertThat(result.status()).isNull();
        });
    }

    private Long provision(String cpf, String email) {
        return provision("Cutover User", cpf, email);
    }

    private Long provision(String name, String cpf, String email) {
        return provisioningService.provision(
                new UserRegistrationData(name, null, email, cpf,
                        null, "5511999999999", null,
                        Instant.parse("2026-08-01T12:00:00Z")),
                "hash"
        );
    }
}
