package com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock;

import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicantAlreadyBlockedException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicantBlockNotFoundException;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplicantBlock;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicantBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MembershipApplicantBlockService {

    private final MembershipApplicantBlockRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public boolean isBlocked(String cpf) {
        return repository.existsActiveByCpf(normalizeCpf(cpf));
    }

    @Transactional
    public void block(
            String cpf,
            String reason,
            Long blockedByUserId,
            Instant blockedAt
    ) {
        String normalizedCpf = normalizeCpf(cpf);

        if (repository.existsActiveByCpf(normalizedCpf)) {
            throw new MembershipApplicantAlreadyBlockedException(normalizedCpf);
        }

        repository.save(MembershipApplicantBlock.create(
                normalizedCpf,
                reason,
                blockedAt,
                blockedByUserId
        ));
    }

    @Transactional
    public void unblock(String cpf, Long unblockedByUserId) {
        String normalizedCpf = normalizeCpf(cpf);
        MembershipApplicantBlock block = repository
                .findActiveByCpf(normalizedCpf)
                .orElseThrow(() -> new MembershipApplicantBlockNotFoundException(normalizedCpf));

        block.unblock(unblockedByUserId, Instant.now(clock));
        repository.save(block);
    }

    private static String normalizeCpf(String cpf) {
        return cpf.replaceAll("\\D", "");
    }
}
