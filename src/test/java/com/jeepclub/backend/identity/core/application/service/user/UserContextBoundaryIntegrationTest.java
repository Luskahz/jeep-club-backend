package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.iam.authentication.core.domain.enums.AuthenticationAccessStatus;
import com.jeepclub.backend.iam.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserField;
import com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.iam.identity.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.iam.identity.core.application.service.user.AdminUserService;
import com.jeepclub.backend.iam.identity.infra.exception.user.InvalidUserSortFieldException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserContextBoundaryIntegrationTest {

    @Autowired private UserRegistration userRegistration;
    @Autowired private AuthenticationAccountRepository accountRepository;
    @Autowired private UserQuery userQuery;
    @Autowired private AdminUserService adminUserService;

    @Test
    void administrationReadsUserAndDisablesBothAggregatesAtomically() {
        Long userId = provision("74185296355", "cutover@example.com");

        AdminUserResult listed = adminUserService.findAll(
                new AdminUserFilter(userId, null, null, null, null, null,
                        null, null, null, null, null, null, null),
                EnumSet.allOf(AdminUserField.class),
                PageRequest.of(0, 10, Sort.by("updatedAt"))
        ).getContent().get(0);

        assertThat(listed.id()).isEqualTo(userId);
        assertThat(listed.email()).isEqualTo("cutover@example.com");
        assertThat(listed.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(adminUserService.findById(userId)).isEqualTo(listed);

        AdminUserResult disabled = adminUserService.disable(userId);

        assertThat(disabled.status()).isEqualTo(UserStatus.DISABLED);
        assertThat(userQuery.isAdministrativelyActive(userId)).isFalse();
        assertThat(accountRepository.findByIdentityId(userId).orElseThrow().getAccessStatus())
                .isEqualTo(AuthenticationAccessStatus.DISABLED);

        AdminUserResult enabled = adminUserService.enable(userId);
        assertThat(enabled.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(accountRepository.findByIdentityId(userId).orElseThrow().getAccessStatus())
                .isEqualTo(AuthenticationAccessStatus.ENABLED);
    }

    @Test
    void pendingFirstAccessUserRemainsAdministrativelyActive() {
        Instant now = Instant.parse("2026-08-01T12:00:00Z");
        Long userId = userRegistration.createPendingFirstAccess(
                new UserRegistrationData("Pending", null, null, "24681357928",
                        null, null, null, now),
                "raw-password"
        );

        assertThat(userQuery.isAdministrativelyActive(userId)).isTrue();
        assertThat(userQuery.findAdministrativelyActiveUserIds()).contains(userId);
        assertThat(accountRepository.findByIdentityId(userId).orElseThrow().getCredentialStatus())
                .isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
    }

    @Test
    void administrativeReadModelKeepsDatabasePaginationFiltersSearchSortAndSparseFields() {
        provision("Read Model Alpha", "31415926590", "alpha-read-model@example.com");
        provision("Read Model Beta", "27182818205", "beta-read-model@example.com");

        Page<AdminUserResult> page = adminUserService.findAll(
                new AdminUserFilter(null, null, null, "read-model@example.com", null, null,
                        null, UserStatus.ACTIVE, null, null, null, null, "read model"),
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

    @Test
    void rejectsSortFieldsThatDoNotBelongToUser() {
        assertThatThrownBy(() -> adminUserService.findAll(
                new AdminUserFilter(null, null, null, null, null, null,
                        null, null, null, null, null, null, null),
                EnumSet.allOf(AdminUserField.class),
                PageRequest.of(0, 10, Sort.by("credentialStatus"))
        )).isInstanceOf(InvalidUserSortFieldException.class);
    }

    private Long provision(String cpf, String email) {
        return provision("Cutover User", cpf, email);
    }

    private Long provision(String name, String cpf, String email) {
        return userRegistration.createWithPermanentCredential(
                new UserRegistrationData(name, null, email, cpf,
                        null, "5511999999999", null,
                        Instant.parse("2026-08-01T12:00:00Z")),
                "raw-password"
        );
    }
}
