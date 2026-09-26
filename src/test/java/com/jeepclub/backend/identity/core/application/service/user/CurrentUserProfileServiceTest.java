package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.exception.UserEmailAlreadyInUseException;
import com.jeepclub.backend.iam.identity.core.application.exception.UserConflictException;
import com.jeepclub.backend.iam.identity.core.application.service.user.CurrentUserProfileService;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import com.jeepclub.backend.iam.identity.core.port.ProfileImagePort;
import com.jeepclub.backend.iam.identity.core.application.command.ProfileUpdate;
import com.jeepclub.backend.iam.identity.core.application.exception.UserRgAlreadyInUseException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
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
import static com.jeepclub.backend.iam.identity.core.application.command.ProfileUpdate.Field.omitted;
import static com.jeepclub.backend.iam.identity.core.application.command.ProfileUpdate.Field.provided;

@ExtendWith(MockitoExtension.class)
class CurrentUserProfileServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");
    @Mock UserRepository repository;
    @Mock ProfileImagePort images;
    private CurrentUserProfileService service;

    @BeforeEach
    void setUp() {
        service = new CurrentUserProfileService(repository, Clock.fixed(NOW, ZoneOffset.UTC),
                images);
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
    void profilePhotoRequiresStoredObjectBeforeReplacingReference() {
        String key = "images/2026/09/24/550e8400-e29b-41d4-a716-446655440000.png";
        User user = user(null);
        when(repository.findByIdForUpdate(42L)).thenReturn(Optional.of(user));
        doThrow(new StorageObjectNotFoundException()).when(images).requireExisting(key);

        assertThatThrownBy(() -> service.updateProfilePhoto(42L, key))
                .isInstanceOf(StorageObjectNotFoundException.class);
        verify(repository, never()).saveAndFlush(any());

        doNothing().when(images).requireExisting(key);
        when(repository.saveAndFlush(user)).thenReturn(user);
        assertThat(service.updateProfilePhoto(42L, key).profilePhotoStorageKey()).isEqualTo(key);
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

    @Test
    void emptyPatchDoesNotSaveOrTouchMedia() {
        User user = user(null);
        when(repository.findByIdForUpdate(42L)).thenReturn(Optional.of(user));
        assertThat(service.updateProfile(42L, new ProfileUpdate(omitted(), omitted(), omitted(), omitted(), omitted()))
                .updatedAt()).isNull();
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(images);
    }

    @Test
    void missingIdentityIsNotFoundBeforeMediaOrPersistence() {
        assertThatThrownBy(() -> service.updateProfile(404L,
                new ProfileUpdate(provided("Name"), omitted(), omitted(), omitted(), omitted())))
                .isInstanceOf(UserNotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(images);
    }

    @Test
    void concurrentRgConflictDuringFlushIsTranslated() {
        User user = user(null);
        when(repository.findByIdForUpdate(42L)).thenReturn(Optional.of(user));
        UserConflictException conflict = new UserConflictException(new IllegalStateException("constraint"));
        when(repository.saveAndFlush(user)).thenThrow(conflict);
        assertThatThrownBy(() -> service.updateProfile(42L,
                new ProfileUpdate(omitted(), omitted(), provided("22.333.444-5"), omitted(), omitted())))
                .isInstanceOf(UserRgAlreadyInUseException.class).hasCause(conflict);
        verify(repository).existsByRgAndIdNot("223334445", 42L);
        verifyNoInteractions(images);
    }
}
