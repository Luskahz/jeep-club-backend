package com.jeepclub.backend.iam.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.exception.UserEmailAlreadyInUseException;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.iam.identity.core.application.exception.UserConflictException;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CurrentUserProfileService {

    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public UserDetails updateEmail(Long userId, String email) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        String normalizedEmail = User.normalizeEmail(email);
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("email is required");
        }
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
            throw new UserEmailAlreadyInUseException();
        }

        user.updateEmail(normalizedEmail, Instant.now(clock));
        try {
            return UserQueryService.toDetails(userRepository.saveAndFlush(user));
        } catch (UserConflictException exception) {
            throw new UserEmailAlreadyInUseException(exception);
        }
    }
}
