package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.me.MeResponseDTO;
import com.jeepclub.backend.authentication.core.application.results.MeResult;
import com.jeepclub.backend.authentication.core.application.services.MeService;
import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Autenticação",
        description = "Endpoints relacionados à autenticação e gerenciamento de sessão."
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class MeController {

    private final MeService meService;

    @Operation(
            summary = "Consultar usuário autenticado",
            description = "Retorna informações básicas do usuário autenticado e da sessão atual."
    )
    @GetMapping("/me")
    public ResponseEntity<MeResponseDTO> getMe(
            @NotNull Authentication authentication
    ) {
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalArgumentException("Autenticação inválida");
        }

        MeResult result = meService.me(
                principal.getUserId(),
                principal.getSessionId(),
                principal.getAccessTokenExpiresAt()
        );

        MeResponseDTO response = new MeResponseDTO(
                result.userId(),
                result.sessionId(),
                result.sessionActive(),
                result.expiresInSeconds()
        );

        return ResponseEntity.ok(response);
    }
}