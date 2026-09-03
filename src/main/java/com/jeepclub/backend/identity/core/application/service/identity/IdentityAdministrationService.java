package com.jeepclub.backend.identity.core.application.service.identity;

import com.jeepclub.backend.identity.api.module.IdentityAdministration;
import com.jeepclub.backend.identity.api.module.IdentityDetails;
import com.jeepclub.backend.identity.api.module.exception.IdentityAlreadyDisabledException;
import com.jeepclub.backend.identity.api.module.exception.IdentityNotDisabledException;
import com.jeepclub.backend.identity.api.module.exception.IdentityNotFoundException;
import com.jeepclub.backend.identity.core.domain.model.Identity;
import com.jeepclub.backend.identity.api.module.spi.IdentityAuthenticationAdministrationPort;
import com.jeepclub.backend.identity.core.repository.IdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
class IdentityAdministrationService implements IdentityAdministration {

    private final IdentityRepository identityRepository;
    private final IdentityAuthenticationAdministrationPort authenticationAdministrationPort;

    @Override
    @Transactional
    public IdentityDetails disable(Long identityId, Instant now) {
        Identity identity = findForUpdate(identityId);
        try {
            identity.disable(now);
        } catch (com.jeepclub.backend.identity.core.domain.exception.IdentityAlreadyDisabledException exception) {
            throw new IdentityAlreadyDisabledException(identityId, exception);
        }

        Identity saved = identityRepository.save(identity);
        authenticationAdministrationPort.disableAuthentication(identityId, now);
        return IdentityQueryService.toDetails(saved);
    }

    @Override
    @Transactional
    public IdentityDetails enable(Long identityId, Instant now) {
        Identity identity = findForUpdate(identityId);
        try {
            identity.enable(now);
        } catch (com.jeepclub.backend.identity.core.domain.exception.IdentityNotDisabledException exception) {
            throw new IdentityNotDisabledException(identityId, exception);
        }

        Identity saved = identityRepository.save(identity);
        authenticationAdministrationPort.enableAuthentication(identityId, now);
        return IdentityQueryService.toDetails(saved);
    }

    private Identity findForUpdate(Long identityId) {
        return identityRepository.findByIdForUpdate(identityId)
                .orElseThrow(() -> new IdentityNotFoundException(identityId));
    }
}
