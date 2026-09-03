package com.jeepclub.backend.identity.core.application.service.identity;

import com.jeepclub.backend.identity.api.module.IdentityRegistration;
import com.jeepclub.backend.identity.api.module.IdentityRegistrationData;
import com.jeepclub.backend.identity.api.module.exception.IdentityRegistrationConflictException;
import com.jeepclub.backend.identity.core.application.exception.IdentityConflictException;
import com.jeepclub.backend.identity.core.domain.model.Identity;
import com.jeepclub.backend.identity.core.repository.IdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class IdentityRegistrationService implements IdentityRegistration {

    private final IdentityRepository identityRepository;

    @Override
    @Transactional
    public Long create(IdentityRegistrationData data) {
        Identity identity = Identity.create(
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
            return identityRepository.create(identity).getId();
        } catch (IdentityConflictException exception) {
            throw new IdentityRegistrationConflictException(exception);
        }
    }
}
