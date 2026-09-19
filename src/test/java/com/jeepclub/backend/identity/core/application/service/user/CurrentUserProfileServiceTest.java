package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.exception.UserEmailAlreadyInUseException;
import com.jeepclub.backend.iam.identity.core.application.exception.UserConflictException;
import com.jeepclub.backend.iam.identity.core.application.service.user.CurrentUserProfileService;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
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
class CurrentUserProfileServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");
    @Mock UserRepository repository;
    private CurrentUserProfileService service;

    @BeforeEach
    void setUp() {
        service = new CurrentUserProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void authenticatedIdentityCanAddAndNormalizeEmail() {
        User user = user(null);
        when(repository.findByIdForUpdate(42L)).thenReturn(Optional.of(user));
        when(repository.saveAndFlush(user)).thenReturn(user);

        var result = service.updateEmail(42L, "  USER@Example.COM ");

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.email()).isEqualTo("user@example.com");
        assertThat(result.updatedAt()).isEqualTo(NOW);
        verify(repository).existsByEmailAndIdNot("user@example.com", 42L);
    }

    @Test
    void anotherUsersEmailIsRejected() {
        when(repository.findByIdForUpdate(42L)).thenReturn(Optional.of(user(null)));
        when(repository.existsByEmailAndIdNot("used@example.com", 42L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateEmail(42L, "used@example.com"))
                .isInstanceOf(UserEmailAlreadyInUseException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void persistenceConflictDuringFlushIsReportedAsEmailAlreadyInUse() {
        User user = user(null);
        when(repository.findByIdForUpdate(42L)).thenReturn(Optional.of(user));
        when(repository.existsByEmailAndIdNot("used@example.com", 42L)).thenReturn(false);
        when(repository.saveAndFlush(user)).thenThrow(new UserConflictException(new IllegalStateException()));

        assertThatThrownBy(() -> service.updateEmail(42L, "used@example.com"))
                .isInstanceOf(UserEmailAlreadyInUseException.class);
    }

    private User user(String email) {
        return User.reconstitute(
                42L, "User", null, email, "52998224725", null, null, null,
                com.jeepclub.backend.iam.identity.api.module.UserStatus.ACTIVE,
                NOW.minusSeconds(3600), null, null
        );
    }
}
