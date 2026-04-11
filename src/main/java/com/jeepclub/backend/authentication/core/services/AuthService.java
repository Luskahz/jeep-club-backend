package com.jeepclub.backend.authentication.core.services;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.domain.model.exception.CpfNotFoundException;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Casos de Uso (Use Cases) e Serviço de Domínio para Autenticação.
 * Seguindo a Arquitetura Hexagonal, esta classe do Core não possui anotações como @Service do Spring.
 * Ela é instanciada e injetada na camada de configuração (Infra).
 */
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;


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

    @Transactional
    public AuthTokens login(@NotNull String cpf, String senha) {
        Instant now = Instant.now();
        User user = userRepository.findByCpf(cpf)
                .orElseThrow(() ->
                        new CpfNotFoundException("User not found for provided employee number")
                );

        if (!passwordHasher.matches(senha, user.getPasswordHash())) {
            user.registerFailedLogin();
            userRepository.save(user);
            throw new SecurityException("wrong password");
        }

        Session session = findSessionByUserAndDeviceIdAndSessionStatus(user, deviceId, SessionStatus.ACTIVE)
                .map(existingSession -> {
                    if(existingSession.isValid(now)){
                        existingSession.updateIpAddress(ip, now);
                        sessionRepository.save(existingSession);
                        return existingSession;
                    }
                    return sessionRegister(
                            user, deviceId, ip
                    );
                })
                .orElseGet(() -> sessionRegister(user, deviceId, ip));



        IssuedRefreshToken issuedRefreshToken = refreshTokenRegister(session);
        IssuedAccessToken issuedAccessToken = jwtService.generateAccessToken(user, session);
        long expiresInSeconds = Math.max(Duration.between(now, issuedAccessToken.expiresAt()).getSeconds(), 0);

        user.recordSuccessfulLogin(now);
        userRepository.save(user);
        return new AuthTokens(
                issuedRefreshToken.rawToken(),
                issuedAccessToken.token(),
                expiresInSeconds
        );
    }
    public User registerUser(String username, Long employeeId, String password, String ip){
        if(!userCanBeCreated(employeeId)) throw new IllegalStateException("the Employee is not available to an new user");
        return userRegister(username, employeeId, password, ip);
    }
}
