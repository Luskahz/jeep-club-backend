package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserIdNotFoundException;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegisterAndAuthenticateService {

    private final RegisterUserService userService;
    private final UserRepository userRepository;
    private final TokenIssuanceService tokenIssuanceService;
    private final Clock clock;

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
        User user = userService.registerUser(
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
}
