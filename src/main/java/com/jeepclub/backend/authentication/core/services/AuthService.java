package com.jeepclub.backend.authentication.core.services;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

/**
 * Casos de Uso (Use Cases) e Serviço de Domínio para Autenticação.
 * Seguindo a Arquitetura Hexagonal, esta classe do Core não possui anotações como @Service do Spring.
 * Ela é instanciada e injetada na camada de configuração (Infra).
 */
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TokenService tokenService;


    /**
     * Registra um novo sócio ou administrador validando regras cruciais de negócio.
     */
    public User registerUser(String name, LocalDate birthData, String email, String cpf, String rg, 
                             String rawPassword, String phoneNumber) {
        
        // 1. Regra de Negócio: CPF não pode ser duplicado
        if (userRepository.existsByCpf(cpf)) {
            throw new IllegalArgumentException("O CPF informado já está em uso.");
        }

        // TODO: Em implementações de produção, a geração do Hash deve ocorrer através 
        // de um PasswordEncoder (Bcrypt) repassando sua interface no construtor deste serviço.
        String passwordHash = "ENCRYPTED_" + rawPassword; 

        // 2. Modelo Rico do Domínio gera as datas de criação, formatação e status inicial
        User newUser = User.create(name, birthData, email, cpf, rg, passwordHash, phoneNumber);
        
        // 3. Porta de Saída acionada
        return userRepository.save(newUser);
    }
}
