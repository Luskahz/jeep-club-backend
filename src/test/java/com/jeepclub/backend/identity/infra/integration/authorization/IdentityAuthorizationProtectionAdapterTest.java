package com.jeepclub.backend.identity.infra.integration.authorization;

import com.jeepclub.backend.iam.authorization.api.module.role.RoleQuery;
import com.jeepclub.backend.iam.identity.infra.integration.authorization.IdentityAuthorizationProtectionAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityAuthorizationProtectionAdapterTest {

    @Mock
    private RoleQuery roleQuery;

    private IdentityAuthorizationProtectionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new IdentityAuthorizationProtectionAdapter(roleQuery);
    }

    @Test
    void delegatesHasRootRoleToRoleQuery() {
        when(roleQuery.hasRootRole(15L)).thenReturn(true);
        when(roleQuery.hasRootRole(16L)).thenReturn(false);

        assertThat(adapter.hasRootRole(15L)).isTrue();
        assertThat(adapter.hasRootRole(16L)).isFalse();
    }

    @Test
    void rejectsNullUserId() {
        assertThatThrownBy(() -> adapter.hasRootRole(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userId cannot be null");
    }
}
