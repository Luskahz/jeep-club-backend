package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.core.application.services.LogoutService;
import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Autenticação",
        description = "Endpoints relacionados à autenticação e gerenciamento de sessão."
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class LogoutController {

    private final LogoutService logoutService;

    @Operation(
            summary = "Encerrar sessão do usuário autenticado",
            description = "Encerra as sessões ativas do usuário autenticado com base no token de acesso informado."
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @NotNull Authentication auth
    ) {
        if (!(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalArgumentException("Autenticação inválida");
        }

        logoutService.logout(principal.getUserId());

        return ResponseEntity.noContent().build();
    }
}