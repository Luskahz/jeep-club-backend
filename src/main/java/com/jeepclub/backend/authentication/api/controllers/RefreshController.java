package com.jeepclub.backend.authentication.api.controllers;

import com.jeepclub.backend.authentication.api.dtos.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dtos.refresh.RefreshRequestDTO;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.services.RefreshService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class RefreshController {

    private final RefreshService refreshService;

    @PostMapping("/refresh")
    public AuthTokenResponseDTO refresh(
            @RequestBody @Valid @NotNull RefreshRequestDTO request
    ) {
        AuthTokens tokens = refreshService.refresh(request.refreshToken());

        return new AuthTokenResponseDTO(
                tokens.refreshToken(),
                tokens.accessToken(),
                tokens.expiresInSeconds()
        );
    }
}