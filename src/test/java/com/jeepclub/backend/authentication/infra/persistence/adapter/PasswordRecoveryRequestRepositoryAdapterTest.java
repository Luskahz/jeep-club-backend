package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.iam.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.iam.authentication.infra.persistence.adapter.PasswordRecoveryRequestRepositoryAdapter;
import com.jeepclub.backend.iam.authentication.infra.persistence.entity.PasswordRecoveryRequestEntity;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.PasswordRecoveryRequestJpaRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryRequestRepositoryAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-15T14:00:00Z");
    private static final String TOKEN_HASH = "token-hash-123";

    @Mock
    private PasswordRecoveryRequestJpaRepository jpaRepository;

    @InjectMocks
    private PasswordRecoveryRequestRepositoryAdapter adapter;

    private PasswordRecoveryRequestEntity entity;

    @BeforeEach
    void setUp() {
        entity = new PasswordRecoveryRequestEntity();
        entity.setId(1L);
        entity.setUserId(10L);
        entity.setTokenHash(TOKEN_HASH);
        entity.setCreatedAt(CREATED_AT);
        entity.setExpiresAt(EXPIRES_AT);
        entity.setStatus(PasswordRecoveryRequestStatus.OPEN);
        entity.setMethod(PasswordRecoveryRequestMethod.EMAIL_TOKEN);
    }

    @Test
    @DisplayName("save: persiste a entidade e retorna o modelo de domínio correspondente")
    void shouldSaveAndReturnDomainModel() {
        PasswordRecoveryRequest domain = PasswordRecoveryRequest.reconstitute(
                1L, 10L, TOKEN_HASH, EXPIRES_AT, CREATED_AT, null, null,
                PasswordRecoveryRequestStatus.OPEN, PasswordRecoveryRequestMethod.EMAIL_TOKEN
        );

        when(jpaRepository.save(any(PasswordRecoveryRequestEntity.class))).thenReturn(entity);

        PasswordRecoveryRequest saved = adapter.save(domain);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getTokenHash()).isEqualTo(TOKEN_HASH);
        var captured = org.mockito.ArgumentCaptor.forClass(PasswordRecoveryRequestEntity.class);
        verify(jpaRepository).save(captured.capture());
        assertThat(captured.getValue().getId()).isEqualTo(entity.getId());
        assertThat(captured.getValue().getUserId()).isEqualTo(entity.getUserId());
        assertThat(captured.getValue().getTokenHash()).isEqualTo(entity.getTokenHash());
        assertThat(captured.getValue().getCreatedAt()).isEqualTo(entity.getCreatedAt());
        assertThat(captured.getValue().getExpiresAt()).isEqualTo(entity.getExpiresAt());
        assertThat(captured.getValue().getStatus()).isEqualTo(entity.getStatus());
        assertThat(captured.getValue().getMethod()).isEqualTo(entity.getMethod());
        assertThat(captured.getValue().getResolvedAt()).isEqualTo(entity.getResolvedAt());
        assertThat(captured.getValue().getCancelledAt()).isEqualTo(entity.getCancelledAt());
    }

    @Test
    @DisplayName("findByTokenHash: mapeia para domínio quando encontrado")
    void shouldFindByTokenHash() {
        when(jpaRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(entity));

        Optional<PasswordRecoveryRequest> result = adapter.findByTokenHash(TOKEN_HASH);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        verify(jpaRepository).findByTokenHash(TOKEN_HASH);
    }

    @Test
    @DisplayName("findUserIdByTokenHash: delega para o JPA repository")
    void shouldFindUserIdByTokenHash() {
        when(jpaRepository.findUserIdByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(10L));

        Optional<Long> result = adapter.findUserIdByTokenHash(TOKEN_HASH);

        assertThat(result).contains(10L);
        verify(jpaRepository).findUserIdByTokenHash(TOKEN_HASH);
    }

    @Test
    @DisplayName("findOpenByUserId: busca solicitação aberta não expirada")
    void shouldFindOpenByUserId() {
        when(jpaRepository.findFirstByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(10L), eq(PasswordRecoveryRequestStatus.OPEN), eq(CREATED_AT)))
                .thenReturn(Optional.of(entity));

        Optional<PasswordRecoveryRequest> result = adapter.findOpenByUserId(10L, CREATED_AT);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(PasswordRecoveryRequestStatus.OPEN);
        verify(jpaRepository).findFirstByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                10L, PasswordRecoveryRequestStatus.OPEN, CREATED_AT
        );
    }

    @Test
    @DisplayName("findOpenByUserIdAndMethod: busca solicitação aberta por método")
    void shouldFindOpenByUserIdAndMethod() {
        when(jpaRepository.findFirstByUserIdAndStatusAndMethodAndExpiresAtAfterOrderByCreatedAtDesc(
                eq(10L), eq(PasswordRecoveryRequestStatus.OPEN), eq(PasswordRecoveryRequestMethod.EMAIL_TOKEN), eq(CREATED_AT)))
                .thenReturn(Optional.of(entity));

        Optional<PasswordRecoveryRequest> result = adapter.findOpenByUserIdAndMethod(
                10L, PasswordRecoveryRequestMethod.EMAIL_TOKEN, CREATED_AT
        );

        assertThat(result).isPresent();
        assertThat(result.get().getMethod()).isEqualTo(PasswordRecoveryRequestMethod.EMAIL_TOKEN);
    }

    @Test
    @DisplayName("findAll e findByUserId: lista e mapeia solicitações")
    void shouldFindAllAndFindByUserId() {
        when(jpaRepository.findAll()).thenReturn(List.of(entity));
        when(jpaRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(entity));

        List<PasswordRecoveryRequest> all = adapter.findAll();
        List<PasswordRecoveryRequest> byUser = adapter.findByUserId(10L);

        assertThat(all).hasSize(1);
        assertThat(byUser).hasSize(1);
        verify(jpaRepository).findAll();
        verify(jpaRepository).findByUserIdOrderByCreatedAtDesc(10L);
    }
}
