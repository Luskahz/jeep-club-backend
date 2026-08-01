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
public class MembershipApplicationService {

    private final MembershipApplicationRepository repository;
    private final Clock clock;

    @Transactional
    public EnsureMembershipApplicationResult ensure(
            String name,
            String cpf,
            String email,
            String phoneNumber,
            String message
    ) {
        String normalizedCpf = cpf.replaceAll("\\D", "");

        return repository.findOpenByCpf(normalizedCpf)
                .map(EnsureMembershipApplicationResult::existing)
                .orElseGet(() -> create(
                        name,
                        normalizedCpf,
                        email,
                        phoneNumber,
                        message
                ));
    }

    private EnsureMembershipApplicationResult create(
            String name,
            String cpf,
            String email,
            String phoneNumber,
            String message
    ) {
        Instant now = Instant.now(clock);

        MembershipApplication application = MembershipApplication.create(
                name,
                cpf,
                normalizeEmail(email),
                phoneNumber.replaceAll("\\D", ""),
                normalizeNullable(message),
                now
        );

        MembershipApplication saved = repository.save(application);

        return EnsureMembershipApplicationResult.created(saved);
    }

    private static String normalizeEmail(String email) {
        return email == null || email.isBlank()
                ? null
                : email.trim().toLowerCase();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}