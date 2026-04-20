package com.jeepclub.backend.authentication.core.services;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;


import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.domain.model.exception.CpfNotFoundException;
import com.jeepclub.backend.authentication.core.domain.model.exception.InvalidPasswordException;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.repositories.SessionRepository;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;

import com.jeepclub.backend.authentication.core.domain.model.LogoutResult;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.domain.model.exception.CpfNotFoundException;
import com.jeepclub.backend.authentication.core.domain.model.exception.InvalidPasswordException;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.repositories.SessionRepository;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;

import java.time.Instant;
import java.time.LocalDate;
/**
 * Casos de Uso (Use Cases) e Serviço de Domínio para Autenticação.
 * Seguindo a Arquitetura Hexagonal, esta classe do Core não possui anotações como @Service do Spring.
 */

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordHasher passwordHasher;
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

    @Transactional
    public AuthTokens login(String cpf, String senha) {
        Instant now = Instant.now();

        User user = userRepository.findByCpf(cpf)
                .orElseThrow(() -> new CpfNotFoundException("CPF não encontrado"));

        if (!passwordHasher.matches(senha, user.getPasswordHash())) {
            user.registerFailedLogin();
            userRepository.save(user);
            throw new InvalidPasswordException();
        }

        String newRefreshToken = tokenService.generateRefreshToken(user);
        Instant refreshExpiresAt = now.plusSeconds(tokenService.getRefreshTokenExpiresInSeconds());

        // stateful: reutiliza sessão ativa ou cria uma nova
        Session session = sessionRepository.findActiveByUserId(user.getId())
                .map(existing -> {
                    if (existing.isValid(now)) {
                        existing.renew(newRefreshToken, refreshExpiresAt);
                        return existing;
                    }
                    return Session.create(user.getId(), newRefreshToken, refreshExpiresAt);
                })
                .orElseGet(() -> Session.create(user.getId(), newRefreshToken, refreshExpiresAt));

        sessionRepository.save(session);

        String accessToken = tokenService.generateAccessToken(user);
        long expiresInSeconds = tokenService.getAccessTokenExpiresInSeconds();

        user.recordSuccessfulLogin(now);
        userRepository.save(user);

        return new AuthTokens(newRefreshToken, accessToken, expiresInSeconds);
    }

    @Transactional
    public LogoutResult logout(Long userId) {


        Session session = sessionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Sessão não encontrada"));

        userRepository.findById(session.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        session.revoke();

        sessionRepository.save(session);

        return new LogoutResult("Logout realizado com sucesso");
    }



}