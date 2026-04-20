package com.jeepclub.backend.authentication.api.controllers;

import com.jeepclub.backend.authentication.api.dtos.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dtos.login.UserLoginRequest;
import com.jeepclub.backend.authentication.core.services.AuthService;
import com.jeepclub.backend.authentication.core.services.AuthTokens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * necessario melhorar este docs @joao
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth/login")
public class LoginController {

    private final AuthService authService;


    /**
     * necessario melhorar este docs @joao
     */
    @PostMapping("/login")
    public AuthTokenResponseDTO login(@RequestBody @Valid @NotNull UserLoginRequest request) {
        AuthTokens tokens = authService.login(request.cpf(), request.senha());
        return new AuthTokenResponseDTO(
                tokens.refreshToken(),
                tokens.accessToken(),
                tokens.expiresInSeconds()
        );
    }
}
