package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserService userService;
    private final SessionService sessionService;

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

        return sessionService.authenticateRegisteredUser(user);
    }
}
