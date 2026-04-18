package com.jeepclub.backend.authentication.core.services;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;


import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

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
    @Transactional public MeResponse me(
            @NotNull Long userId,
            @NotNull Long sessionId,
            Instant accessTokenExpiresAt
    ) {
        Instant now = Instant.now();
        RefreshToken existingToken = findRefreshTokenByToken(refreshToken)
                .orElseThrow(() -> new SecurityException("Invalid refresh token"));
        if (!existingToken.isValid(now)) {
            throw new SecurityException("Refresh token invalid");
        }

        User user = userRepository.findById(existingToken.getSession().getUserId())
                .orElseThrow(() -> new SecurityException("User not found in session"));

        IssuedRefreshToken issuedRefreshToken = rotateRefreshTokenFromExisting(existingToken, now);
        IssuedAccessToken issuedAccessToken = jwtService.generateAccessToken(user, issuedRefreshToken.refreshToken().getSession());
        long expiresInSeconds = Math.max(Duration.between(now, issuedAccessToken.expiresAt()).getSeconds(), 0);
        return new AuthTokens(
                issuedRefreshToken.rawToken(),
                issuedAccessToken.token(),
                expiresInSeconds
        );
    }
}