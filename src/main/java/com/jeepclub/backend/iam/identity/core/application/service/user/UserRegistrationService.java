package com.jeepclub.backend.iam.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.AdminUserCreationResult;
import com.jeepclub.backend.iam.identity.api.module.UserActivationMethod;
import com.jeepclub.backend.iam.identity.api.module.spi.AdminUserCredentialProvisioningPort;
import com.jeepclub.backend.platform.storage.image.ImageMediaService;
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
    private final ImageMediaService images;
    private final AdminUserCredentialProvisioningPort adminCredentialProvisioningPort;

    @Override
    @Transactional
    public AdminUserCreationResult createAdministratively(UserRegistrationData data, UserActivationMethod method) {
        if (method == null) {
            throw new IllegalArgumentException("activationMethod is required");
        }
        if (method == UserActivationMethod.EMAIL_INVITATION && User.normalizeEmail(data.email()) == null) {
            throw new IllegalArgumentException("email is required for invitation");
        }
        Long userId = createUser(data);
        String temporaryPassword = adminCredentialProvisioningPort.provision(userId, method, data.now());
        return new AdminUserCreationResult(userId, method, temporaryPassword,
                method == UserActivationMethod.EMAIL_INVITATION ? "QUEUED" : null);
    }

    @Override
    @Transactional
    public UserAuthenticationTokens registerAndAuthenticate(
            UserRegistrationData data,
            String rawPassword
    ) {
        Long userId = createUser(data);
        return authenticationProvisioningPort.provisionAndAuthenticate(
                userId,
                data.name(),
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
        images.requireExisting(data.profilePhotoStorageKey());
        User user = User.create(
                data.name(),
                data.birthDate(),
                data.email(),
                data.cpf(),
                data.rg(),
                data.phoneNumber(),
                data.profilePhotoStorageKey(),
                data.now()
        );

        try {
            return userRepository.create(user).getId();
        } catch (UserConflictException exception) {
            throw new UserRegistrationConflictException(exception);
        }
    }
}
