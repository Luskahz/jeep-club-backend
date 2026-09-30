package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.iam.identity.core.application.service.user.UserProfileService;
import com.jeepclub.backend.iam.identity.core.domain.model.*;
import com.jeepclub.backend.iam.identity.core.repository.UserProfileRepository;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserProfileServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserProfileRepository profiles = mock(UserProfileRepository.class);
    private final UserProfileService service = new UserProfileService(users, profiles);

    @Test
    void unknownUsersCannotReadOrCreateProfiles() {
        assertThatThrownBy(() -> service.get(7L)).isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> service.replace(7L, null, null)).isInstanceOf(UserNotFoundException.class);
        verifyNoInteractions(profiles);
    }

    @Test
    void absentProfileIsPendingWithoutWritingAndCompleteProfileIsNotPending() {
        when(users.existsById(7L)).thenReturn(true);
        assertThat(service.hasPendingProfileCompletion(7L)).isTrue();
        verify(profiles, never()).save(any());
        when(profiles.findByUserId(7L)).thenReturn(Optional.of(new UserProfile(7L,
                new WorkProfile("Mechanic", "Garage"),
                new ResidentialAddress("12345678", "Street", "1", null, "Center", "City", "SP"))));
        assertThat(service.hasPendingProfileCompletion(7L)).isFalse();
    }

    @Test
    void locksParentBeforeUpsertEvenWhenProfileDoesNotYetExist() {
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(mock(User.class)));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var saved = service.replace(7L, new WorkProfile(" Mechanic ", null), null);
        assertThat(saved.userId()).isEqualTo(7L);
        assertThat(saved.profileCompletionPending()).isTrue();
        var ordered = inOrder(users, profiles);
        ordered.verify(users).findByIdForUpdate(7L);
        ordered.verify(profiles).save(saved);
    }
}
