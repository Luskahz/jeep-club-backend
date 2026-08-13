package com.jeepclub.backend.memberships.infra.persistence.jpa;

import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplicantBlock;
import com.jeepclub.backend.memberships.infra.persistence.adapter.MembershipApplicantBlockRepositoryAdapter;
import com.jeepclub.backend.memberships.infra.persistence.entity.MembershipApplicantBlockEntity;
import com.jeepclub.backend.memberships.infra.persistence.entity.MembershipApplicationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(MembershipApplicantBlockRepositoryAdapter.class)
class MembershipApplicantBlockJpaRepositoryTest {

    private static final String CPF = "52998224725";
    private static final Instant NOW = Instant.parse("2026-08-08T12:00:00Z");

    @Autowired
    private MembershipApplicantBlockRepositoryAdapter repository;
    @Autowired
    private MembershipApplicantBlockJpaRepository jpaRepository;
    @Autowired
    private MembershipApplicationJpaRepository applicationJpaRepository;

    @Test
    void findsOnlyActiveBlockAndPreservesUnblockedHistory() {
        MembershipApplicantBlock first = repository.save(block(NOW.minusSeconds(3600), 10L));

        assertThat(repository.existsActiveByCpf(CPF)).isTrue();
        assertThat(repository.findActiveByCpf(CPF)).get()
                .extracting(MembershipApplicantBlock::getReason)
                .isEqualTo("Dados inconsistentes");

        first.unblock(20L, NOW);
        repository.save(first);
        jpaRepository.flush();
        MembershipApplicantBlock second = repository.save(block(NOW.plusSeconds(3600), 30L));
        jpaRepository.flush();

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(repository.findActiveByCpf(CPF)).get()
                .extracting(MembershipApplicantBlock::getId)
                .isEqualTo(second.getId());
        assertThat(jpaRepository.count()).isEqualTo(2);
    }

    @Test
    void databasePreventsTwoActiveBlocksForSameCpf() {
        repository.save(block(NOW, 10L));
        jpaRepository.flush();

        assertThatThrownBy(() -> {
                    repository.save(block(NOW.plusSeconds(1), 20L));
                    jpaRepository.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectedApplicationDoesNotPreventAnotherApplicationForSameCpf() {
        applicationJpaRepository.saveAndFlush(application(
                "first@example.com",
                MembershipApplicationStatus.REJECTED
        ));
        applicationJpaRepository.saveAndFlush(application(
                "second@example.com",
                MembershipApplicationStatus.PENDING
        ));

        assertThat(applicationJpaRepository.count()).isEqualTo(2);
    }

    private MembershipApplicantBlock block(Instant blockedAt, Long userId) {
        return MembershipApplicantBlock.create(
                CPF,
                "Dados inconsistentes",
                blockedAt,
                userId
        );
    }

    private MembershipApplicationEntity application(
            String email,
            MembershipApplicationStatus status
    ) {
        MembershipApplicationEntity application = new MembershipApplicationEntity();
        application.setName("Candidate");
        application.setCpf(CPF);
        application.setEmail(email);
        application.setPhoneNumber("11999999999");
        application.setStatus(status);
        application.setRequestedAt(NOW);
        application.setUpdatedAt(NOW);
        return application;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = MembershipApplicantBlockJpaRepository.class)
    @EntityScan(basePackageClasses = MembershipApplicantBlockEntity.class)
    static class TestConfiguration {
    }
}
