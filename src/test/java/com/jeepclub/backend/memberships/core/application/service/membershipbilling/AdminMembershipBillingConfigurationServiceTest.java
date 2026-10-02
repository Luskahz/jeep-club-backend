package com.jeepclub.backend.memberships.core.application.service.membershipbilling;

import com.jeepclub.backend.billing.api.module.ChargeDefinitionQuery;
import com.jeepclub.backend.memberships.core.application.exception.InvalidMembershipChargeDefinitionException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipBillingConfigurationNotFoundException;
import com.jeepclub.backend.memberships.core.domain.model.MembershipBillingConfiguration;
import com.jeepclub.backend.memberships.core.repository.MembershipBillingConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMembershipBillingConfigurationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");

    @Mock
    private MembershipBillingConfigurationRepository repository;
    @Mock
    private ChargeDefinitionQuery definitionQuery;

    private AdminMembershipBillingConfigurationService service;

    @BeforeEach
    void setUp() {
        service = new AdminMembershipBillingConfigurationService(
                repository,
                definitionQuery,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldSupportMissingConfiguration() {
        when(repository.findCurrent()).thenReturn(Optional.empty());

        assertThat(service.getCurrent()).isEmpty();
    }

    @Test
    void shouldCreateConfiguration() {
        when(definitionQuery.isActive(10L)).thenReturn(true);
        when(repository.findCurrent()).thenReturn(Optional.empty());

        MembershipBillingConfiguration result = service.configure(10L, true);

        assertThat(result.getChargeDefinitionId()).isEqualTo(10L);
        assertThat(result.isEnforcementEnabled()).isTrue();
        assertThat(result.getCreatedAt()).isEqualTo(NOW);
        verify(repository).save(result);
    }

    @Test
    void shouldReplaceDefinitionAndDisableEnforcement() {
        MembershipBillingConfiguration current = MembershipBillingConfiguration.create(10L, true, NOW.minusSeconds(60));
        when(definitionQuery.isActive(20L)).thenReturn(true);
        when(repository.findCurrent()).thenReturn(Optional.of(current));

        MembershipBillingConfiguration result = service.configure(20L, false);

        assertThat(result.getChargeDefinitionId()).isEqualTo(20L);
        assertThat(result.isEnforcementEnabled()).isFalse();
        assertThat(result.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldEnableWithoutChangingDefinition() {
        MembershipBillingConfiguration current = MembershipBillingConfiguration.create(10L, false, NOW.minusSeconds(60));
        when(repository.findCurrent()).thenReturn(Optional.of(current));
        when(definitionQuery.isActive(10L)).thenReturn(true);

        MembershipBillingConfiguration result = service.setEnforcementEnabled(true);

        assertThat(result.getChargeDefinitionId()).isEqualTo(10L);
        assertThat(result.isEnforcementEnabled()).isTrue();
    }

    @Test
    void shouldRejectEnablingWhenStoredDefinitionBecameInactive() {
        MembershipBillingConfiguration current = MembershipBillingConfiguration.create(10L, false, NOW.minusSeconds(60));
        when(repository.findCurrent()).thenReturn(Optional.of(current));
        when(definitionQuery.isActive(10L)).thenReturn(false);

        assertThatThrownBy(() -> service.setEnforcementEnabled(true))
                .isInstanceOf(InvalidMembershipChargeDefinitionException.class);
        assertThat(current.isEnforcementEnabled()).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void shouldAllowDisablingWithoutRevalidatingStoredDefinition() {
        MembershipBillingConfiguration current = MembershipBillingConfiguration.create(10L, true, NOW.minusSeconds(60));
        when(repository.findCurrent()).thenReturn(Optional.of(current));

        MembershipBillingConfiguration result = service.setEnforcementEnabled(false);

        assertThat(result.isEnforcementEnabled()).isFalse();
        verifyNoInteractions(definitionQuery);
    }

    @Test
    void shouldRejectInactiveOrMissingDefinition() {
        when(definitionQuery.isActive(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.configure(99L, true))
                .isInstanceOf(InvalidMembershipChargeDefinitionException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectEnforcementChangeBeforeConfigurationExists() {
        when(repository.findCurrent()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setEnforcementEnabled(true))
                .isInstanceOf(MembershipBillingConfigurationNotFoundException.class);
    }
}
