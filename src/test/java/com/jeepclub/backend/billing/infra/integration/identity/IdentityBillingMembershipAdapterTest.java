package com.jeepclub.backend.billing.infra.integration.identity;

import com.jeepclub.backend.identity.api.module.IdentityQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityBillingMembershipAdapterTest {

    @Mock
    private IdentityQuery identityQuery;

    @InjectMocks
    private IdentityBillingMembershipAdapter adapter;

    @Test
    void resolvesBillingTargetsFromAdministrativeIdentityStatus() {
        when(identityQuery.findAdministrativelyActiveIdentityIds())
                .thenReturn(List.of(10L, 20L));
        when(identityQuery.isAdministrativelyActive(10L)).thenReturn(true);

        assertThat(adapter.findActiveMemberUserIds()).containsExactly(10L, 20L);
        assertThat(adapter.existsActiveMemberByUserId(10L)).isTrue();

        verify(identityQuery).findAdministrativelyActiveIdentityIds();
        verify(identityQuery).isAdministrativelyActive(10L);
    }
}
