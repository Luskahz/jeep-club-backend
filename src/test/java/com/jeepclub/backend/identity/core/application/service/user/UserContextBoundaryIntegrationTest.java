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
    @Autowired private com.jeepclub.backend.iam.identity.api.module.UserAdministration lifecycle;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(UserStatus.class)
    void administrativeStatusFilterDoesNotConfuseAvailabilityWithCredentialState(UserStatus status) {
        Long id = provision("Status filter", "39053344705", "status-filter@example.com");
        Instant transition = Instant.parse("2026-08-02T12:00:00Z");
        if (status == UserStatus.INACTIVE) lifecycle.disable(id, transition);
        if (status == UserStatus.ARCHIVED) lifecycle.archive(id, id, transition);
        var matching = adminUserService.findAll(new AdminUserFilter(id,null,null,null,null,null,null,
                status,null,null,null,null,null),EnumSet.of(AdminUserField.ID,AdminUserField.STATUS),PageRequest.of(0,10));
        assertThat(matching.getContent()).singleElement().satisfies(user -> {
            assertThat(user.id()).isEqualTo(id);
            assertThat(user.status()).isEqualTo(status);
            assertThat(user.cpf()).isNull();
            assertThat(user.email()).isNull();
        });
        var other = status == UserStatus.ACTIVE ? UserStatus.INACTIVE : UserStatus.ACTIVE;
        assertThat(adminUserService.findAll(new AdminUserFilter(id,null,null,null,null,null,null,
                other,null,null,null,null,null),EnumSet.of(AdminUserField.ID),PageRequest.of(0,10))).isEmpty();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"0,0,1","1,2,0","-2,-1,0"})
    void creationRangeIsInclusiveAndActuallyFiltersRows(long fromOffset,long toOffset,long expected) {
        Long id = provision("Range filter", "39053344705", "range-filter@example.com");
        Instant created = Instant.parse("2026-08-01T12:00:00Z");
        var result = adminUserService.findAll(new AdminUserFilter(id,null,null,null,null,null,null,null,
                created.plusSeconds(fromOffset),created.plusSeconds(toOffset),null,null,null),
                EnumSet.of(AdminUserField.ID),PageRequest.of(0,10));
        assertThat(result.getTotalElements()).isEqualTo(expected);
        assertThat(result.getContent()).hasSize((int)expected);
        if(expected == 1) assertThat(result.getContent().get(0).id()).isEqualTo(id);
    }

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

        assertThat(disabled.status()).isEqualTo(UserStatus.INACTIVE);
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
