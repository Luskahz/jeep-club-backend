package com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock;

import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicantAlreadyBlockedException;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplicantBlock;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicantBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMembershipApplicantBlockServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-08T12:00:00Z");
    private static final String CPF = "52998224725";

    @Mock
    private MembershipApplicantBlockRepository repository;

    private AdminMembershipApplicantBlockService service;

    @BeforeEach
    void setUp() {
        service = new AdminMembershipApplicantBlockService(
                repository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsBlockWithNormalizedCpfAndAuditData() {
        ArgumentCaptor<MembershipApplicantBlock> captor =
                ArgumentCaptor.forClass(MembershipApplicantBlock.class);

        service.block("529.982.247-25", "Dados inconsistentes", 10L, NOW);

        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCpf()).isEqualTo(CPF);
        assertThat(captor.getValue().getReason()).isEqualTo("Dados inconsistentes");
        assertThat(captor.getValue().getBlockedAt()).isEqualTo(NOW);
        assertThat(captor.getValue().getBlockedByUserId()).isEqualTo(10L);
    }

    @Test
    void activeBlockPreventsAnotherBlock() {
        when(repository.existsActiveByCpf(CPF)).thenReturn(true);

        assertThatThrownBy(() -> service.block(CPF, "Motivo", 10L, NOW))
                .isInstanceOf(MembershipApplicantAlreadyBlockedException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void unblockPreservesBlockAndRecordsAuditFields() {
        MembershipApplicantBlock block = MembershipApplicantBlock.create(
                CPF,
                "Dados inconsistentes",
                NOW.minusSeconds(3600),
                10L
        );
        when(repository.findActiveByCpf(CPF)).thenReturn(Optional.of(block));

        service.unblock("529.982.247-25", 20L);

        assertThat(block.getUnblockedAt()).isEqualTo(NOW);
        assertThat(block.getUnblockedByUserId()).isEqualTo(20L);
        verify(repository).save(block);
    }
}
