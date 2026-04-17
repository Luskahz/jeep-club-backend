package com.jeepclub.backend.authentication.infra.config;

import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;
import com.jeepclub.backend.authentication.core.services.AuthService;
import com.jeepclub.backend.authentication.core.services.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfiguration {

    @Bean
    public AuthService authService(UserRepository userRepository,
                                   PasswordHasher passwordHasher,
                                   TokenService tokenService) {
        return new AuthService(userRepository, passwordHasher, tokenService);
    }
}
