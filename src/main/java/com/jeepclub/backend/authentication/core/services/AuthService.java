package com.jeepclub.backend.authentication.core.services;

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

@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    public User registerUser(String name, LocalDate birthData, String email, String cpf, String rg,
                             String rawPassword, String phoneNumber) {

        if (userRepository.existsByCpf(cpf)) {
            throw new IllegalArgumentException("O CPF informado já está em uso.");
        }

        String passwordHash = passwordHasher.hash(rawPassword);
        User newUser = User.create(name, birthData, email, cpf, rg, passwordHash, phoneNumber);
        return userRepository.save(newUser);
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
