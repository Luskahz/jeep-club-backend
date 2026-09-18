package com.jeepclub.backend.authorization.core.application.query;

import com.jeepclub.backend.iam.authorization.core.application.query.AuthorizationRoleQueryService;
import com.jeepclub.backend.iam.authorization.core.domain.model.Role;
import com.jeepclub.backend.iam.authorization.core.repository.RoleRepository;
import com.jeepclub.backend.iam.authorization.core.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationRoleQueryServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private AuthorizationRoleQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new AuthorizationRoleQueryService(roleRepository, userRoleRepository);
    }

    @Test
    void hasRootRoleReturnsTrueWhenUserIsAssignedToActiveRootRole() {
        Long userId = 10L;
        Long rootRoleId = 1L;
        Role rootRole = Role.createRoot("ROOT", "Root role", Instant.now());
        // set ID via reconstitution or mock
        Role reconstituted = Role.reconstitute(
                rootRoleId,
                "ROOT",
                "Root role",
                rootRole.getKind(),
                rootRole.getStatus(),
                rootRole.getCreatedAt(),
                rootRole.getUpdatedAt(),
                null
        );

        when(roleRepository.findRoot()).thenReturn(Optional.of(reconstituted));
        when(userRoleRepository.existsByUserIdAndRoleId(userId, rootRoleId)).thenReturn(true);

        assertThat(queryService.hasRootRole(userId)).isTrue();
    }

    @Test
    void hasRootRoleReturnsFalseWhenUserIsNotAssignedToRootRole() {
        Long userId = 20L;
        Long rootRoleId = 1L;
        Role rootRole = Role.reconstitute(
                rootRoleId,
                "ROOT",
                "Root role",
                Role.createRoot("ROOT", "Root role", Instant.now()).getKind(),
                Role.createRoot("ROOT", "Root role", Instant.now()).getStatus(),
                Instant.now(),
                Instant.now(),
                null
        );

        when(roleRepository.findRoot()).thenReturn(Optional.of(rootRole));
        when(userRoleRepository.existsByUserIdAndRoleId(userId, rootRoleId)).thenReturn(false);

        assertThat(queryService.hasRootRole(userId)).isFalse();
    }

    @Test
    void hasRootRoleReturnsFalseWhenRootRoleNotFound() {
        when(roleRepository.findRoot()).thenReturn(Optional.empty());

        assertThat(queryService.hasRootRole(30L)).isFalse();
    }

    @Test
    void hasRootRoleReturnsFalseWhenUserIdIsNull() {
        assertThat(queryService.hasRootRole(null)).isFalse();
    }

    @Test
    void existsActiveRoleByIdAndFindUserIdsByRoleIdWorkAsExpected() {
        Long roleId = 5L;
        Role activeRole = Role.reconstitute(
                roleId,
                "CUSTOM_ROLE",
                "Custom",
                Role.create("CUSTOM_ROLE", "Custom", Instant.now()).getKind(),
                Role.create("CUSTOM_ROLE", "Custom", Instant.now()).getStatus(),
                Instant.now(),
                Instant.now(),
                null
        );

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(activeRole));
        when(userRoleRepository.findUserIdsByRoleId(roleId)).thenReturn(List.of(1L, 2L, 3L));

        assertThat(queryService.existsActiveRoleById(roleId)).isTrue();
        assertThat(queryService.findUserIdsByRoleId(roleId)).containsExactly(1L, 2L, 3L);
    }
}
