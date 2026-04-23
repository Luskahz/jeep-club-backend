package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.core.application.services.LogoutService;
import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class LogoutController {

    private final LogoutService logoutService;

    @PostMapping("/logout")
    public void logout(
            @NotNull Authentication auth
    ) {
        if (!(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalArgumentException("Autenticação inválida");
        }

        logoutService.logout(principal.getUserId());
    }
}