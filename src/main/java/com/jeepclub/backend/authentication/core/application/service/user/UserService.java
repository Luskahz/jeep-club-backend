package com.jeepclub.backend.authentication.core.application.service.user;

import com.jeepclub.backend.authentication.core.application.exceptions.user.RegistrationConflictException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.account.AuthenticationAccountConflictException;
import com.jeepclub.backend.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.authentication.core.application.service.internal.TokenIssuanceService;
import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.authentication.core.application.service.account.AuthenticationAccountProvisioningService;
import com.jeepclub.backend.identity.api.module.IdentityRegistrationData;
import com.jeepclub.backend.identity.core.application.exception.IdentityConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthenticationAccountRepository accountRepository;
    private final AuthenticationAccountProvisioningService provisioningService;
    private final PasswordHasher passwordHasher;
    private final TokenIssuanceService tokenIssuanceService;
    private final Clock clock;

    public Long register(
            String name,
            LocalDate birthDate,
            String email,
            String cpf,
            String rg,
            String passwordRaw,
            String phoneNumber
    ) {
        return registerUser(
                name,
                birthDate,
                email,
                cpf,
                rg,
                passwordRaw,
                phoneNumber
        );
    }

    @Transactional
    public AuthTokens registerAndAuthenticate(
            String name,
            LocalDate birthDate,
            String email,
            String cpf,
            String rg,
            String rawPassword,
            String phoneNumber
    ) {
        Long identityId = registerUser(
                name,
                birthDate,
                email,
                cpf,
                rg,
                rawPassword,
                phoneNumber
        );

        AuthenticationAccount account = accountRepository.findByIdentityIdForUpdate(identityId)
                .orElseThrow(() -> new UserIdNotFoundException("Registered user not found."));
        Instant now = Instant.now(clock);
        AuthTokens tokens = tokenIssuanceService.issue(account, now);
        account.recordSuccessfulLogin(now);
        accountRepository.save(account);
        return tokens;
    }

    private Long registerUser(
            String name,
            LocalDate birthDate,
            String email,
            String cpf,
            String rg,
            String passwordRaw,
            String phoneNumber
    ) {
        Instant now = Instant.now(clock);

        String passwordHash = passwordHasher.hash(passwordRaw);
        try {
            return provisioningService.provision(
                    new IdentityRegistrationData(
                            name, birthDate, email, cpf, rg, phoneNumber, null, now
                    ),
                    passwordHash
            );
        } catch (IdentityConflictException | AuthenticationAccountConflictException exception) {
            throw new RegistrationConflictException();
        }
    }
}
