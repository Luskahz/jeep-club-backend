package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.authentication.infra.persistence.jpa.PasswordChangeChallengeJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordChangeChallengeRepositoryAdapterTest {

    @Mock
    private PasswordChangeChallengeJpaRepository jpaRepository;

    @InjectMocks
    private PasswordChangeChallengeRepositoryAdapter adapter;

    @Test
    @DisplayName("invalidateActiveByUserId delega para o update em lote do repositório JPA")
    void shouldDelegateInvalidationToBulkUpdate() {
        Instant now = Instant.parse("2026-05-21T18:00:00Z");

        when(jpaRepository.invalidateActiveByUserId(7L, now))
                .thenReturn(3);

        int updatedCount = adapter.invalidateActiveByUserId(7L, now);

        assertEquals(3, updatedCount);
        verify(jpaRepository).invalidateActiveByUserId(7L, now);
    }
}
