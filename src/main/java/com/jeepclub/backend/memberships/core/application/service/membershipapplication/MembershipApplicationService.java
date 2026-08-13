package com.jeepclub.backend.memberships.core.application.service.membershipapplication;

import com.jeepclub.backend.authentication.api.module.user.UserQuery;
import com.jeepclub.backend.memberships.core.application.exception.MembershipApplicantBlockedException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipCpfAlreadyRegisteredException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipEmailAlreadyInUseException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipEmailAlreadyRegisteredException;
import com.jeepclub.backend.memberships.core.application.result.EnsureMembershipRequestResult;
import com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock.MembershipApplicantBlockService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
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
    private final MembershipApplicantBlockService membershipApplicantBlockService;
    private final UserQuery userQuery;
    private final Clock clock;

    @Transactional
    public EnsureMembershipRequestResult ensure(
            String name,
            String cpf,
            String email,
            String phoneNumber,
            String message
    ) {
        String normalizedCpf = cpf.replaceAll("\\D", "");
        String normalizedEmail = normalizeEmail(email);

        if (membershipApplicantBlockService.isBlocked(normalizedCpf)) {
            throw new MembershipApplicantBlockedException();
        }

        return repository.findByCpfAndStatus(
                        normalizedCpf,
                        MembershipApplicationStatus.PENDING
                )
                .map(EnsureMembershipRequestResult::existing)
                .orElseGet(() -> {
                    validateAvailability(
                            normalizedCpf,
                            normalizedEmail
                    );

                    return create(
                            name,
                            normalizedCpf,
                            normalizedEmail,
                            phoneNumber,
                            message
                    );
                });
    }

    private void validateAvailability(String cpf, String email) {
        if (userQuery.existsByCpf(cpf)) {
            throw new MembershipCpfAlreadyRegisteredException(cpf);
        }

        if (repository.existsByEmailAndStatus(
                email,
                MembershipApplicationStatus.PENDING
        )) {
            throw new MembershipEmailAlreadyInUseException(email);
        }

        if (userQuery.existsByEmail(email)) {
            throw new MembershipEmailAlreadyRegisteredException(email);
        }
    }

    private EnsureMembershipRequestResult create(
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
                email,
                normalizePhoneNumber(phoneNumber),
                normalizeNullable(message),
                now
        );

        MembershipApplication saved = repository.save(application);

        return EnsureMembershipRequestResult.created(saved);
    }

    private static String normalizeEmail(String email) {
        return email == null || email.isBlank()
                ? null
                : email.trim().toLowerCase();
    }

    private static String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber == null || phoneNumber.isBlank()
                ? null
                : phoneNumber.replaceAll("\\D", "");
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
