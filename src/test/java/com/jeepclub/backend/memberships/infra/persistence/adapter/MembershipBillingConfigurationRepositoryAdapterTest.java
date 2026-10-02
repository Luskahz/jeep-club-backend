package com.jeepclub.backend.memberships.infra.persistence.adapter;

import com.jeepclub.backend.memberships.core.domain.model.MembershipBillingConfiguration;
import com.jeepclub.backend.memberships.infra.persistence.jpa.MembershipBillingConfigurationJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MembershipBillingConfigurationRepositoryAdapter.class)
class MembershipBillingConfigurationRepositoryAdapterTest {

    @Autowired
    private MembershipBillingConfigurationRepositoryAdapter adapter;
    @Autowired
    private MembershipBillingConfigurationJpaRepository jpaRepository;

    @Test
    void shouldPersistAndReplaceTheSingleCurrentConfiguration() {
        Instant createdAt = Instant.parse("2026-09-21T12:00:00Z");
        MembershipBillingConfiguration saved = adapter.save(
                MembershipBillingConfiguration.create(10L, false, createdAt)
        );

        saved.replaceChargeDefinition(20L, createdAt.plusSeconds(60));
        saved.setEnforcementEnabled(true, createdAt.plusSeconds(60));
        adapter.save(saved);

        assertThat(jpaRepository.count()).isEqualTo(1);
        assertThat(adapter.findCurrent()).hasValueSatisfying(configuration -> {
            assertThat(configuration.getChargeDefinitionId()).isEqualTo(20L);
            assertThat(configuration.isEnforcementEnabled()).isTrue();
        });
    }
}
