package com.jeepclub.backend.iam.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.iam.identity.api.module.UserAuthenticationTokens;
import com.jeepclub.backend.iam.identity.api.module.exception.UserRegistrationConflictException;
import com.jeepclub.backend.iam.identity.api.module.spi.UserAuthenticationProvisioningPort;
import com.jeepclub.backend.iam.identity.core.application.exception.UserConflictException;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class UserRegistrationService implements UserRegistration {

    private final UserRepository userRepository;
    private final UserAuthenticationProvisioningPort authenticationProvisioningPort;

    @Override
    @Transactional
    public UserAuthenticationTokens registerAndAuthenticate(
            UserRegistrationData data,
            String rawPassword
    ) {
        Long userId = createUser(data);
        return authenticationProvisioningPort.provisionAndAuthenticate(
                userId,
                rawPassword,
                data.now()
        );
    }

    @Override
    @Transactional
    public Long createWithPermanentCredential(
            UserRegistrationData data,
            String rawPassword
    ) {
        Long userId = createUser(data);
        authenticationProvisioningPort.provisionPermanent(userId, rawPassword, data.now());
        return userId;
    }

    @Override
    @Transactional
    public Long createPendingFirstAccess(
            UserRegistrationData data,
            String rawPassword
    ) {
        Long userId = createUser(data);
        authenticationProvisioningPort.provisionPendingFirstAccess(userId, rawPassword, data.now());
        return userId;
    }

    private Long createUser(UserRegistrationData data) {
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
