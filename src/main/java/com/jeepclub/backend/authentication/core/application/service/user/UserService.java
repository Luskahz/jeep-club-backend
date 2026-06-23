package com.jeepclub.backend.authentication.core.application.service.user;

import com.jeepclub.backend.authentication.core.application.exceptions.user.RegistrationConflictException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.authentication.core.application.service.internal.TokenIssuanceService;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuanceService tokenIssuanceService;
    private final Clock clock;

    public User register(
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
        User user = registerUser(
                name,
                birthDate,
                email,
                cpf,
                rg,
                rawPassword,
                phoneNumber
        );

        User lockedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new UserIdNotFoundException("Registered user not found."));
        Instant now = Instant.now(clock);
        AuthTokens tokens = tokenIssuanceService.issue(lockedUser, now);
        lockedUser.recordSuccessfulLogin(now);
        userRepository.save(lockedUser);
        return tokens;
    }

    private User registerUser(
            String name,
            LocalDate birthDate,
            String email,
            String cpf,
            String rg,
            String passwordRaw,
            String phoneNumber
    ) {
        Instant now = Instant.now(clock);

        if (userRepository.existsByCpf(cpf)
                || userRepository.existsByEmail(email)
                || userRepository.existsByRg(rg)) {
            throw new RegistrationConflictException();
        }

        String passwordHash = passwordHasher.hash(passwordRaw);

        User newUser = User.create(
                name,
                birthDate,
                email,
                cpf,
                rg,
                passwordHash,
                phoneNumber,
                now
        );

        return userRepository.create(newUser);
    }
}
