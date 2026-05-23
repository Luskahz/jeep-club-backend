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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
@Validated
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
    public ResponseEntity<MeResponseDTO> getMe(
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();


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

        return ResponseEntity.ok(
                new MeResponseDTO(
                        result.userId(),
                        result.sessionId(),
                        result.sessionActive(),
                        result.expiresInSeconds(),
                        authorities
                )
        );
    }
}