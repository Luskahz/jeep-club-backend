package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.iam.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.iam.authentication.core.domain.model.Session;
import com.jeepclub.backend.iam.authentication.infra.persistence.adapter.SessionRepositoryAdapter;
import com.jeepclub.backend.iam.authentication.infra.persistence.entity.SessionEntity;
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
class SessionRepositoryAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-15T20:00:00Z");

    @Mock
    private SessionJpaRepository jpaRepository;

    @InjectMocks
    private SessionRepositoryAdapter adapter;

    private SessionEntity entity;

    @BeforeEach
    void setUp() {
        entity = new SessionEntity();
        entity.setId(1L);
        entity.setUserId(10L);
        entity.setCreatedAt(CREATED_AT);
        entity.setExpiresAt(EXPIRES_AT);
        entity.setStatus(SessionStatus.ACTIVE);
    }

    @Test
    @DisplayName("save: persiste a entidade e retorna o modelo de domínio")
    void shouldSaveAndReturnDomain() {
        Session domain = Session.reconstitute(1L, 10L, CREATED_AT, EXPIRES_AT, null, SessionStatus.ACTIVE);

        when(jpaRepository.save(any(SessionEntity.class))).thenReturn(entity);

        Session saved = adapter.save(domain);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        var captured = org.mockito.ArgumentCaptor.forClass(SessionEntity.class);
        verify(jpaRepository).save(captured.capture());
        assertThat(captured.getValue().getId()).isEqualTo(entity.getId());
        assertThat(captured.getValue().getUserId()).isEqualTo(entity.getUserId());
        assertThat(captured.getValue().getCreatedAt()).isEqualTo(entity.getCreatedAt());
        assertThat(captured.getValue().getExpiresAt()).isEqualTo(entity.getExpiresAt());
        assertThat(captured.getValue().getLoggedOutAt()).isEqualTo(entity.getLoggedOutAt());
        assertThat(captured.getValue().getStatus()).isEqualTo(entity.getStatus());
    }

    @Test
    @DisplayName("findById e findUserIdById: busca e mapeia corretamente")
    void shouldFindByIdAndFindUserIdById() {
        when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(jpaRepository.findUserIdById(1L)).thenReturn(Optional.of(10L));

        Optional<Session> session = adapter.findById(1L);
        Optional<Long> userId = adapter.findUserIdById(1L);

        assertThat(session).isPresent();
        assertThat(session.get().getId()).isEqualTo(1L);
        assertThat(userId).contains(10L);
        verify(jpaRepository).findById(1L);
        verify(jpaRepository).findUserIdById(1L);
    }

    @Test
    @DisplayName("findActiveByUserId: busca sessão ativa mais recente")
    void shouldFindActiveByUserId() {
        when(jpaRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(10L, SessionStatus.ACTIVE))
                .thenReturn(Optional.of(entity));

        Optional<Session> session = adapter.findActiveByUserId(10L);

        assertThat(session).isPresent();
        assertThat(session.get().getStatus()).isEqualTo(SessionStatus.ACTIVE);
        verify(jpaRepository).findFirstByUserIdAndStatusOrderByCreatedAtDesc(10L, SessionStatus.ACTIVE);
    }

    @Test
    @DisplayName("findAll e findByUserId: lista e mapeia sessões")
    void shouldFindAllAndFindByUserId() {
        when(jpaRepository.findAll()).thenReturn(List.of(entity));
        when(jpaRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(entity));

        List<Session> all = adapter.findAll();
        List<Session> byUser = adapter.findByUserId(10L);

        assertThat(all).hasSize(1);
        assertThat(byUser).hasSize(1);
        verify(jpaRepository).findAll();
        verify(jpaRepository).findByUserIdOrderByCreatedAtDesc(10L);
    }

    @Test
    @DisplayName("revokeActiveByUserId: delega revogação em lote para o repositório JPA")
    void shouldRevokeActiveByUserId() {
        adapter.revokeActiveByUserId(10L);

        verify(jpaRepository).revokeActiveByUserId(10L);
    }
}
