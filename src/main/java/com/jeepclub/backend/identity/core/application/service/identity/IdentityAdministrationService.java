package com.jeepclub.backend.identity.core.application.service.identity;

import com.jeepclub.backend.identity.api.module.IdentityAdministration;
import com.jeepclub.backend.identity.api.module.IdentityDetails;
import com.jeepclub.backend.identity.core.application.exception.IdentityNotFoundException;
import com.jeepclub.backend.identity.core.domain.model.Identity;
import com.jeepclub.backend.identity.core.repository.IdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
class IdentityAdministrationService implements IdentityAdministration {

    private final IdentityRepository identityRepository;

    @Override
    @Transactional
    public IdentityDetails disable(Long identityId, Instant now) {
        Identity identity = findForUpdate(identityId);
        identity.disable(now);
        return IdentityQueryService.toDetails(identityRepository.save(identity));
    }

    @Override
    @Transactional
    public IdentityDetails enable(Long identityId, Instant now) {
        Identity identity = findForUpdate(identityId);
        identity.enable(now);
        return IdentityQueryService.toDetails(identityRepository.save(identity));
    }

    private Identity findForUpdate(Long identityId) {
        return identityRepository.findByIdForUpdate(identityId)
                .orElseThrow(() -> new IdentityNotFoundException(identityId));
    }
}
