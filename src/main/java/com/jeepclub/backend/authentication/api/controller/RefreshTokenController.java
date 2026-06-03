package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.token.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dto.token.RefreshTokenRequestDTO;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.services.RefreshService;
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
        name = "Authentication - Refresh Tokens",
        description = "Rotação e renovação de tokens de autenticação."
)
public class RefreshTokenController {

    private final RefreshService refreshService;

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar tokens",
            description = "Gera um novo access token e um novo refresh token a partir de um refresh token válido."
    )
    public ResponseEntity<AuthTokenResponseDTO> refresh(
            @RequestBody @Valid RefreshTokenRequestDTO request
    ) {
        AuthTokens tokens = refreshService.refresh(
                request.refreshToken()
        );

        return ResponseEntity.ok(
                AuthTokenResponseDTO.from(tokens)
        );
    }
}