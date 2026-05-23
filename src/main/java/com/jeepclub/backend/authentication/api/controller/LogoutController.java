package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.core.application.services.LogoutService;
import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Logout",
        description = "Encerramento de sessão autenticada."
)
public class LogoutController {

    private final LogoutService logoutService;

    @PostMapping("/logout")
    @Operation(
            summary = "Encerrar sessão",
            description = "Encerra a sessão do usuário autenticado com base no token de acesso informado."
    )
    public ResponseEntity<Void> logout(
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        logoutService.logout(principal.getUserId());

        return ResponseEntity.noContent().build();
    }
}