package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dto.login.LoginRequestDTO;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.services.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Autenticação",
        description = "Endpoints responsáveis por login, registro e renovação de tokens."
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class LoginController {

    private final LoginService loginService;

    @Operation(
            summary = "Autenticar usuário",
            description = "Realiza login com CPF e senha, retornando access token e refresh token."
    )
    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponseDTO> login(
            @RequestBody @Valid @NotNull LoginRequestDTO request
    ) {
        AuthTokens tokens = loginService.login(request.cpf(), request.senha());

        AuthTokenResponseDTO response = new AuthTokenResponseDTO(
                tokens.refreshToken(),
                tokens.accessToken(),
                tokens.expiresInSeconds()
        );

        return ResponseEntity.ok(response);
    }
}