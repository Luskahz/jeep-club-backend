package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dto.login.UserLoginRequest;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.services.LoginService;
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
@RequestMapping("/auth")
public class LoginController {

    private final LoginService loginService;

    /**
     * necessario melhorar este docs @joao
     */
    @PostMapping("/login")
    public AuthTokenResponseDTO login(
            @RequestBody @Valid @NotNull UserLoginRequest request
    ) {
        AuthTokens tokens = loginService.login(request.cpf(), request.senha());
        return new AuthTokenResponseDTO(
                tokens.refreshToken(),
                tokens.accessToken(),
                tokens.expiresInSeconds()
        );
    }
}
