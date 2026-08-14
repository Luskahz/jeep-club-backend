package com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock;

import com.jeepclub.backend.memberships.core.repository.MembershipApplicantBlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipApplicantBlockServiceTest {

    private static final String CPF = "52998224725";

    @Mock
    private MembershipApplicantBlockRepository repository;

    private MembershipApplicantBlockService service;

    @BeforeEach
    void setUp() {
        service = new MembershipApplicantBlockService(repository);
    }

    @Test
    void checksActiveBlockUsingNormalizedCpf() {
        when(repository.existsActiveByCpf(CPF)).thenReturn(true);

        assertThat(service.isBlocked("529.982.247-25")).isTrue();

        verify(repository).existsActiveByCpf(CPF);
    }

}
