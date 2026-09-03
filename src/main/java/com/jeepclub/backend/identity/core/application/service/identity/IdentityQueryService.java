package com.jeepclub.backend.identity.core.application.service.identity;

import com.jeepclub.backend.identity.api.module.IdentityDetails;
import com.jeepclub.backend.identity.api.module.IdentityQuery;
import com.jeepclub.backend.identity.core.domain.model.Identity;
import com.jeepclub.backend.identity.core.repository.IdentityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
class IdentityQueryService implements IdentityQuery {

    private final IdentityRepository identityRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityDetails> findById(Long identityId) {
        return identityRepository.findById(identityId).map(IdentityQueryService::toDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityDetails> findByCpf(String cpf) {
        return identityRepository.findByCpf(cpf).map(IdentityQueryService::toDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long identityId) {
        return identityRepository.existsById(identityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAdministrativelyActiveIdentityIds() {
        return identityRepository.findActiveIds();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdministrativelyActive(Long identityId) {
        return identityRepository.existsActiveById(identityId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCpf(String cpf) {
        return cpf != null && identityRepository.existsByCpf(Identity.normalizeCpf(cpf));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        String normalized = Identity.normalizeEmail(email);
        return normalized != null && identityRepository.existsByEmail(normalized);
    }

    static IdentityDetails toDetails(Identity identity) {
        return new IdentityDetails(
                identity.getId(), identity.getName(), identity.getBirthDate(),
                identity.getEmail(), identity.getCpf(), identity.getRg(),
                identity.getPhoneNumber(), identity.getProfilePhotoUrl(),
                identity.isActive(), identity.getCreatedAt(), identity.getDisabledAt(),
                identity.getUpdatedAt()
        );
    }
}
