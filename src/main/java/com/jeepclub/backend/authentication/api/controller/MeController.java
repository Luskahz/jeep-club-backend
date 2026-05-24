package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.me.MeResponseDTO;
import com.jeepclub.backend.authentication.core.application.results.MeResult;
import com.jeepclub.backend.authentication.core.application.services.MeService;
import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
@Tag(
        name = "Authentication - Me",
        description = "Consulta da sessão autenticada e permissões do usuário."
)
public class MeController {

    private final MeService meService;

    @GetMapping("/me")
    @Operation(
            summary = "Consultar sessão autenticada",
            description = "Retorna os dados da sessão do usuário autenticado e suas permissões atuais."
    )
    public ResponseEntity<MeResponseDTO> getMe(Authentication authentication) {
        Objects.requireNonNull(authentication, "authentication cannot be null");

        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("Authenticated principal is not a valid UserPrincipal.");
        }


        MeResult result = meService.me(
                principal.getUserId(),
                principal.getSessionId(),
                principal.getAccessTokenExpiresAt()
        );

        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();

        return ResponseEntity.ok(MeResponseDTO.from(result, authorities));
    }
}