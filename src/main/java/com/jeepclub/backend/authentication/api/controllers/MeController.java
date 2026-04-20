package com.jeepclub.backend.authentication.api.controllers;

import com.jeepclub.backend.authentication.api.dtos.me.AuthMeResponseDTO;
import com.jeepclub.backend.authentication.core.application.results.MeResponse;
import com.jeepclub.backend.authentication.infra.config.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;




/**
 * Melhorar esse docs vinicius
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class MeController {

    private final AuthService authService;

    /**
     * Endpoint para obter os dados da sessão/token do usuário logado.
     */
    @GetMapping("/me")
    public AuthMeResponseDTO getMe(
            Authentication authentication
    ) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        MeResponse response = authService.me(
                principal.getUserId(),
                principal.getSessionId(),
                principal.getAccessTokenExpiresAt()
        );
        return new AuthMeResponseDTO(
                response.userId(),
                response.sessionId(),
                response.sessionActive(),
                response.expiresInSeconds()
        );
    }
}
