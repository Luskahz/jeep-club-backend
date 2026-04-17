package com.jeepclub.backend.authentication.core.services;

import com.jeepclub.backend.authentication.api.dtos.UserMeResponse;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;


import java.time.LocalDate;

/**
 * Casos de Uso (Use Cases) e Serviço de Domínio para Autenticação.
 * Seguindo a Arquitetura Hexagonal, esta classe do Core não possui anotações como @Service do Spring.
 */

public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
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

    /**
     * Decodifica o token e retorna as informações da sessão do usuário.
     * (Task BACK-XX: Obter usuário autenticado /me)
     */
    public UserMeResponse getUserSessionInfoFromToken(String token) {

        try {
            /* * EXPLICAÇÃO PARA O SEU PROJETO:
             * Na arquitetura hexagonal, o ideal é que você tenha uma porta (Interface)
             * chamada 'TokenService' ou 'TokenProvider'. O AuthService usaria essa interface
             * para extrair os dados sem saber se você usa JWT, Auth0, etc.
             */

            // SIMULAÇÃO: Aqui você chamaria seu decodificador de JWT.
            // Por enquanto, vamos retornar dados mockados (fictícios) para você testar a rota.

            String userId = "USER-777"; // No futuro: jwtService.getSubject(token)
            String sessionId = "SESS-999"; // No futuro: jwtService.getClaim(token, "jti")
            Long expiresIn = 3600L; // No futuro: calcular tempo restante do token

            return new UserMeResponse(userId, sessionId, expiresIn);

        } catch (Exception e) {
            // Caso o token esteja corrompido ou mal formatado
            throw new IllegalArgumentException("Erro ao processar o token de acesso.");
        }
    }
}