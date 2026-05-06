package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dto.refresh.RefreshRequestDTO;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.services.RefreshService;
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
public class RefreshController {

    private final RefreshService refreshService;

    @Operation(
            summary = "Renovar tokens de autenticação",
            description = "Gera um novo access token e um novo refresh token a partir de um refresh token válido."
    )
    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponseDTO> refresh(
            @RequestBody @Valid @NotNull RefreshRequestDTO request
    ) {
        AuthTokens tokens = refreshService.refresh(request.refreshToken());

        AuthTokenResponseDTO response = new AuthTokenResponseDTO(
                tokens.refreshToken(),
                tokens.accessToken(),
                tokens.expiresInSeconds()
        );

        return ResponseEntity.ok(response);
    }
}