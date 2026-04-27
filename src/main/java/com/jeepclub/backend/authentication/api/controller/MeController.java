package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.me.MeResponseDTO;
import com.jeepclub.backend.authentication.core.application.results.MeResult;
import com.jeepclub.backend.authentication.core.application.services.MeService;
import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class    MeController {

    private final MeService meService;

    @GetMapping("/me")
    public MeResponseDTO getMe(Authentication authentication) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        MeResult response = meService.me(
                principal.getUserId(),
                principal.getSessionId(),
                principal.getAccessTokenExpiresAt()
        );

        return new MeResponseDTO(
                response.userId(),
                response.sessionId(),
                response.sessionActive(),
                response.expiresInSeconds()
        );
    }
}