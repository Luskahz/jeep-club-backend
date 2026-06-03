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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - User Registration",
        description = "Registro público de usuários e emissão inicial de tokens."
)
public class UserController {

    private final RegisterService registerService;
    private final LoginService loginService;

    @PostMapping("/register")
    @Operation(
            summary = "Registrar usuário",
            description = "Cria uma nova conta de usuário e retorna os tokens de autenticação."
    )
    public ResponseEntity<AuthTokenResponseDTO> register(
            @RequestBody @Valid RegisterRequestDTO request
    ) {
        User user = registerService.registerUser(
                request.name(),
                request.birthData(),
                request.email(),
                request.cpf(),
                request.rg(),
                request.password(),
                request.phoneNumber()
        );

        AuthTokens tokens = loginService.authenticateRegisteredUser(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthTokenResponseDTO.from(tokens));
    }
}