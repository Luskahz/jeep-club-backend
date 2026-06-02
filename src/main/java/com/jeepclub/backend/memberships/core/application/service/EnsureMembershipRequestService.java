package com.jeepclub.backend.memberships.core.application.service;

import com.jeepclub.backend.memberships.core.application.result.EnsureMembershipRequestResult;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EnsureMembershipRequestService {

    private final MembershipApplicationRepository membershipApplicationRepository;
    private final Clock clock;

    @Transactional
    public EnsureMembershipRequestResult ensure(
            String name,
            String cpf,
            String email,
            String phoneNumber,
            String message
    ) {
        Instant now = Instant.now(clock);
        String normalizedCpf = cpf.replaceAll("[^0-9]", "");

        return membershipApplicationRepository.findByCpf(normalizedCpf)
                .map(EnsureMembershipRequestResult::existing)
                .orElseGet(() -> {
                    MembershipApplication application = MembershipApplication.create(
                            name,
                            normalizedCpf,
                            email,
                            phoneNumber,
                            message,
                            now
                    );

                    MembershipApplication saved = membershipApplicationRepository.save(application);

                    return EnsureMembershipRequestResult.created(saved);
                });
    }
}