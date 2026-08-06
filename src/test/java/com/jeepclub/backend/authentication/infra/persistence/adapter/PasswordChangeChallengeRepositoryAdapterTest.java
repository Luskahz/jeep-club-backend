package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.authentication.infra.persistence.jpa.PasswordChangeChallengeJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordChangeChallengeRepositoryAdapterTest {

    @Mock
    private PasswordChangeChallengeJpaRepository jpaRepository;

    @InjectMocks
    private PasswordChangeChallengeRepositoryAdapter adapter;

    @Test
    @DisplayName("invalidateActiveByUserId delega para o update em lote do repositÃ³rio JPA")
    void shouldDelegateInvalidationToBulkUpdate() {
        Instant now = Instant.parse("2026-05-21T18:00:00Z");

        adapter.invalidateActiveByUserId(7L, now);

        verify(jpaRepository).invalidateActiveByUserId(7L, now);
    }
}
