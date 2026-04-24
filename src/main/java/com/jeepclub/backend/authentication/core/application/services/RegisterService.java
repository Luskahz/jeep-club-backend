package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public User registerUser(String name, LocalDate birthData, String email, String cpf, String rg,
                             String rawPassword, String phoneNumber) {

        if (userRepository.existsByCpf(cpf)) {
            throw new IllegalArgumentException("O CPF informado já está em uso.");
        }

        String passwordHash = passwordHasher.hash(rawPassword);

        User newUser = User.create(name, birthData, email, cpf, rg, passwordHash, phoneNumber);

        return userRepository.save(newUser);
    }
}