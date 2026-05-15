package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dto.register.RegisterRequestDTO;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.services.LoginService;
import com.jeepclub.backend.authentication.core.application.services.RegisterService;
import com.jeepclub.backend.authentication.core.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Autenticação",
        description = "Endpoints relacionados à autenticação e gerenciamento de sessão."
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class RegisterController {

    private final RegisterService registerService;
    private final LoginService loginService;

    @Operation(
            summary = "Registrar usuário",
            description = "Cria uma nova conta de usuário e retorna os tokens de autenticação."
    )
    @PostMapping("/register")
    public ResponseEntity<AuthTokenResponseDTO> register(
            @RequestBody @Valid @NotNull RegisterRequestDTO request
    ) {
        User newUser = registerService.registerUser(
                request.name(),
                request.birthData(),
                request.email(),
                request.cpf(),
                request.rg(),
                request.password(),
                request.phoneNumber()
        );

        AuthTokens tokens = loginService.login(
                newUser.getCpf(),
                request.password()
        );

        AuthTokenResponseDTO response = new AuthTokenResponseDTO(
                tokens.refreshToken(),
                tokens.accessToken(),
                tokens.expiresInSeconds()
        );

        return ResponseEntity.status(201).body(response);
    }
}