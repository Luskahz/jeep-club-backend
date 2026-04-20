package com.jeepclub.backend.authentication.api.controllers;


import com.jeepclub.backend.authentication.api.dtos.logout.LogoutResponseDTO;
import com.jeepclub.backend.authentication.infra.config.UserPrincipal;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * necessario melhorar este docs @Kauan
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class LogoutController {

    private final AuthService authService;

    /**
     * necessario melhorar este docs @Kauan
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponseDTO> logout(
            @NotNull Authentication auth
    ) {
        if(!(auth.getPrincipal() instanceof UserPrincipal principal)){
            throw new IllegalArgumentException("Autentificação inválida");
        }
        var result = authService.logout(principal.getUserId());

        return ResponseEntity.ok(new LogoutResponseDTO(result.message()));
    }
}
