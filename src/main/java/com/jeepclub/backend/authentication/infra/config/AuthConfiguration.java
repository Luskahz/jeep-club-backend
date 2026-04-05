package com.jeepclub.backend.authentication.infra.config;

import com.jeepclub.backend.authentication.core.repositories.UserRepository;
import com.jeepclub.backend.authentication.core.services.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfiguration {

    @Bean
    public AuthService authService(UserRepository userRepository) {
        return new AuthService(userRepository);
    }
}