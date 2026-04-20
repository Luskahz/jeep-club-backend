package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.repositories.SessionRepository;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class RegisterService {

    private final UserRepository userRepository;


    /**
     * Registra um novo sócio ou administrador validando regras cruciais de negócio.
     */
    public User registerUser(String name, LocalDate birthData, String email, String cpf, String rg,
                             String rawPassword, String phoneNumber) {

        // 1. Regra de Negócio: CPF não pode ser duplicado
        if (userRepository.existsByCpf(cpf)) {
            throw new IllegalArgumentException("O CPF informado já está em uso.");
        }

        // TODO: Em produção, usar um PasswordEncoder injetado no construtor.
        String passwordHash = "ENCRYPTED_" + rawPassword;

        // 2. Modelo Rico do Domínio gera as datas de criação e status inicial
        User newUser = User.create(name, birthData, email, cpf, rg, passwordHash, phoneNumber);

        // 3. Porta de Saída acionada
        return userRepository.save(newUser);
    }

}
