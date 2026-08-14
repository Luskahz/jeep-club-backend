package com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock;

import com.jeepclub.backend.memberships.core.repository.MembershipApplicantBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MembershipApplicantBlockService {

    private final MembershipApplicantBlockRepository repository;

    @Transactional(readOnly = true)
    public boolean isBlocked(String cpf) {
        return repository.existsActiveByCpf(normalizeCpf(cpf));
    }

    private static String normalizeCpf(String cpf) {
        return cpf.replaceAll("\\D", "");
    }
}
