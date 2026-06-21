package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.exceptions.user.RegistrationConflictException;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public User registerUser(
            String name,
            LocalDate birthData,
            String email,
            String cpf,
            String rg,
            String passwordRaw,
            String phoneNumber

    ) {
        Instant now = Instant.now(clock);

        if (userRepository.existsByCpf(cpf)) {
            throw new RegistrationConflictException();
        }

        String passwordHash = passwordHasher.hash(passwordRaw);

        User newUser = User.create(name, birthData, email, cpf, rg, passwordHash, phoneNumber, now);

        return userRepository.save(newUser);
    }
}