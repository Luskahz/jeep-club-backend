package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.iam.authentication.core.domain.enums.RefreshTokenStatus;
import com.jeepclub.backend.iam.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.iam.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.iam.authentication.core.domain.model.Session;
import com.jeepclub.backend.iam.authentication.infra.persistence.adapter.RefreshTokenRepositoryAdapter;
import com.jeepclub.backend.iam.authentication.infra.persistence.entity.RefreshTokenEntity;
import com.jeepclub.backend.iam.authentication.infra.persistence.entity.SessionEntity;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.RefreshTokenJpaRepository;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.SessionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-22T12:00:00Z");
    private static final String TOKEN_HASH = "token-hash-xyz";

    @Mock
    private RefreshTokenJpaRepository jpaRepository;

    @Mock
    private SessionJpaRepository sessionJpaRepository;

    @InjectMocks
    private RefreshTokenRepositoryAdapter adapter;

    private Session domainSession;
    private SessionEntity sessionEntity;
    private RefreshTokenEntity tokenEntity;

    @BeforeEach
    void setUp() {
        domainSession = Session.reconstitute(1L, 10L, CREATED_AT, EXPIRES_AT, null, SessionStatus.ACTIVE);

        sessionEntity = new SessionEntity();
        sessionEntity.setId(1L);
        sessionEntity.setUserId(10L);
        sessionEntity.setCreatedAt(CREATED_AT);
        sessionEntity.setExpiresAt(EXPIRES_AT);
        sessionEntity.setStatus(SessionStatus.ACTIVE);

        tokenEntity = new RefreshTokenEntity();
        tokenEntity.setId(100L);
        tokenEntity.setSessionId(1L);
        tokenEntity.setTokenHash(TOKEN_HASH);
        tokenEntity.setCreatedAt(CREATED_AT);
        tokenEntity.setExpiresAt(EXPIRES_AT);
        tokenEntity.setStatus(RefreshTokenStatus.ACTIVE);
    }

    @Test
    @DisplayName("save: persiste a entidade do refresh token e reconstrói o modelo de domínio")
    void shouldSaveAndReturnDomain() {
        RefreshToken domainToken = RefreshToken.reconstitute(
                100L, domainSession, TOKEN_HASH, CREATED_AT, EXPIRES_AT, RefreshTokenStatus.ACTIVE, null
        );

        when(jpaRepository.save(any(RefreshTokenEntity.class))).thenReturn(tokenEntity);

        RefreshToken saved = adapter.save(domainToken);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(100L);
        assertThat(saved.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(saved.getSession().getId()).isEqualTo(1L);
        var captured = org.mockito.ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(jpaRepository).save(captured.capture());
        assertThat(captured.getValue().getId()).isEqualTo(tokenEntity.getId());
        assertThat(captured.getValue().getSessionId()).isEqualTo(tokenEntity.getSessionId());
        assertThat(captured.getValue().getTokenHash()).isEqualTo(tokenEntity.getTokenHash());
        assertThat(captured.getValue().getCreatedAt()).isEqualTo(tokenEntity.getCreatedAt());
        assertThat(captured.getValue().getExpiresAt()).isEqualTo(tokenEntity.getExpiresAt());
        assertThat(captured.getValue().getStatus()).isEqualTo(tokenEntity.getStatus());
        assertThat(captured.getValue().getReplacedByTokenId()).isEqualTo(tokenEntity.getReplacedByTokenId());
    }

    @Test
    @DisplayName("findByTokenHash: busca por hash e mapeia para domínio com sua sessão associada")
    void shouldFindByTokenHash() {
        when(jpaRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(tokenEntity));
        when(sessionJpaRepository.findById(1L)).thenReturn(Optional.of(sessionEntity));

        Optional<RefreshToken> result = adapter.findByTokenHash(TOKEN_HASH);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(100L);
        assertThat(result.get().getSession().getId()).isEqualTo(1L);
        verify(jpaRepository).findByTokenHash(TOKEN_HASH);
    }

    @Test
    @DisplayName("findSessionIdByTokenHash: delega para consulta do JPA repository")
    void shouldFindSessionIdByTokenHash() {
        when(jpaRepository.findSessionIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(1L));

        Optional<Long> sessionId = adapter.findSessionIdByTokenHash(TOKEN_HASH);

        assertThat(sessionId).contains(1L);
        verify(jpaRepository).findSessionIdByTokenHash(TOKEN_HASH);
    }

    @Test
    @DisplayName("findAll: lista todos os refresh tokens hidratando as sessões correspondentes")
    void shouldFindAllWithSessions() {
        when(jpaRepository.findAll()).thenReturn(List.of(tokenEntity));
        when(sessionJpaRepository.findAllById(List.of(1L))).thenReturn(List.of(sessionEntity));

        List<RefreshToken> all = adapter.findAll();

        assertThat(all).hasSize(1);
        assertThat(all.get(0).getId()).isEqualTo(100L);
        verify(jpaRepository).findAll();
        verify(sessionJpaRepository).findAllById(List.of(1L));
    }

    @Test
    @DisplayName("revokeActiveByUserId e revokeActiveBySessionId: delegam para operações de revogação no JPA")
    void shouldDelegateRevocations() {
        adapter.revokeActiveByUserId(10L);
        adapter.revokeActiveBySessionId(1L);

        verify(jpaRepository).revokeActiveByUserId(10L);
        verify(jpaRepository).revokeActiveBySessionId(1L);
    }
}
