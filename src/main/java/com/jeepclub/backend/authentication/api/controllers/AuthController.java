package com.jeepclub.backend.authentication.api.controllers;

import com.jeepclub.backend.authentication.api.dtos.UserRegisterRequest;
import com.jeepclub.backend.authentication.api.dtos.UserRegisterResponse;
import com.jeepclub.backend.authentication.api.dtos.me.AuthMeResponseDTO;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.services.AuthService;
import com.jeepclub.backend.authentication.core.services.AuthTokens;
import com.jeepclub.backend.authentication.api.dtos.login.UserLoginRequest;
import com.jeepclub.backend.authentication.core.services.MeResponse;
import com.jeepclub.backend.authentication.infra.config.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import com.jeepclub.backend.authentication.api.dtos.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dtos.UserRegisterRequest;
import com.jeepclub.backend.authentication.api.dtos.UserRegisterResponse;
import com.jeepclub.backend.authentication.api.dtos.logout.LogoutRequestDTO;
import com.jeepclub.backend.authentication.api.dtos.logout.LogoutResponseDTO;
import com.jeepclub.backend.authentication.core.domain.model.LogoutResult;
import com.jeepclub.backend.authentication.core.services.AuthService;
import com.jeepclub.backend.authentication.core.services.AuthTokens;
import com.jeepclub.backend.authentication.api.dtos.login.UserLoginRequest;
import com.jeepclub.backend.authentication.infra.config.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * Porta Principal / Adaptador de Entrada Primária (Primary Inbound Adapter).
 * Única e exclusiva camada a se preocupar com transporte da web (JSON, HTTP
 * Headers, Códigos de Status).
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class AuthController {

    private final AuthService authService;

    /**
     * Endpoint isolado focando em injetar o fluxo de Registro (Task BACK-81).
     */
    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody UserRegisterRequest request) {

        try {
            // O controlador aciona explicitamente a porta de serviço do núcleo
            User newUser = authService.registerUser(
                    request.name(),
                    request.birthData(),
                    request.email(),
                    request.cpf(),
                    request.rg(),
                    request.password(),
                    request.phoneNumber());

            // Transforma o modelo do domínio num modelo superficial para a Web
            UserRegisterResponse response = UserRegisterResponse.fromDomain(newUser);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // Caso caia na regra de negócio do CPF já existente, retorna erro seguro 400
            // Bad Request
            // (Para arquiteturas completas costuma-se usar o @ControllerAdvice)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public AuthTokenResponseDTO login(@RequestBody @Valid @NotNull UserLoginRequest request) {
        AuthTokens tokens = authService.login(request.cpf(), request.senha());
        return new AuthTokenResponseDTO(
                tokens.refreshToken(),
                tokens.accessToken(),
                tokens.expiresInSeconds()
        );
    }


    /**
     * Endpoint para obter os dados da sessão/token do usuário logado.
     */
    @GetMapping("/me")
    public AuthMeResponseDTO getMe(
            Authentication authentication
    ) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String token = authorizationHeader.replace("Bearer ", "");

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