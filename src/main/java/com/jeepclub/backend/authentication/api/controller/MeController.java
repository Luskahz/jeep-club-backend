package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.me.MeResponseDTO;
import com.jeepclub.backend.authentication.core.application.results.MeResult;
import com.jeepclub.backend.authentication.core.application.services.MeService;
import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
            summary = "Consultar dados da sessão",
            description = "Retorna os dados do usuário autenticado e suas permissões atuais."
    )
    @GetMapping("/me")
    public MeResponseDTO getMe(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        MeResult response = meService.me(
                principal.getUserId(),
                principal.getSessionId(),
                principal.getAccessTokenExpiresAt()
        );

        List<String> authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();

        return new MeResponseDTO(
                response.userId(),
                response.sessionId(),
                response.sessionActive(),
                response.expiresInSeconds(),
                authorities
        );
    }
}