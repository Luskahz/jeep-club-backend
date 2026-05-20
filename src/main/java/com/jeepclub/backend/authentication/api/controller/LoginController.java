package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dto.login.LoginRequestDTO;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.services.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Login",
        description = "Autenticação de usuários e emissão de tokens."
)
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login")
    @Operation(
            summary = "Autenticar usuário",
            description = "Realiza login com CPF e senha, retornando access token e refresh token."
    )
    public ResponseEntity<AuthTokenResponseDTO> login(
            @RequestBody @Valid LoginRequestDTO request
    ) {
        AuthTokens tokens = loginService.login(
                request.cpf(),
                request.senha()
        );

        return ResponseEntity.ok(
                new AuthTokenResponseDTO(
                        tokens.refreshToken(),
                        tokens.accessToken(),
                        tokens.expiresInSeconds()
                )
        );
    }
}