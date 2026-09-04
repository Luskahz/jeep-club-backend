package com.jeepclub.backend.identity.core.application.service.user;

import com.jeepclub.backend.identity.api.module.UserRegistration;
import com.jeepclub.backend.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.identity.api.module.exception.UserRegistrationConflictException;
import com.jeepclub.backend.identity.core.application.exception.UserConflictException;
import com.jeepclub.backend.identity.core.domain.model.User;
import com.jeepclub.backend.identity.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UserRegistrationService implements UserRegistration {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public Long create(UserRegistrationData data) {
        User user = User.create(
                data.name(),
                data.birthDate(),
                data.email(),
                data.cpf(),
                data.rg(),
                data.phoneNumber(),
                data.profilePhotoUrl(),
                data.now()
        );

        try {
            return userRepository.create(user).getId();
        } catch (UserConflictException exception) {
            throw new UserRegistrationConflictException(exception);
        }
    }
}
