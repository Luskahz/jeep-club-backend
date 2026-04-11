package com.jeepclub.backend.authentication.api.controllers;

import com.jeepclub.backend.authentication.api.dtos.AuthMeResponseDTO;
import com.jeepclub.backend.authentication.api.dtos.UserRegisterRequest;
import com.jeepclub.backend.authentication.api.dtos.UserRegisterResponse;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.services.AuthService;
import com.jeepclub.backend.authentication.infra.config.UserPrincipal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Porta Principal / Adaptador de Entrada Primária (Primary Inbound Adapter).
 * Única e exclusiva camada a se preocupar com transporte da web (JSON, HTTP
 * Headers, Códigos de Status).
 */

@RestController
@RequestMapping("/users")
public class AuthController {

    private final AuthService authService;

    // Adicione este construtor aqui!
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

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

    /**
     * Endpoint para retornar os dados da sessão do usuário autenticado (/me).
     */
    @GetMapping("/me")
    public ResponseEntity<AuthMeResponseDTO> me(Authentication authentication) {

        // Extrai o "Principal" do token JWT decodificado pelo Spring Security
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // Calcula os segundos restantes até o token expirar
        long expiresInSeconds = ChronoUnit.SECONDS.between(
                Instant.now(),
                principal.getAccessTokenExpiresAt()
        );

        // Garante que não retorne tempo negativo
        if (expiresInSeconds < 0) {
            expiresInSeconds = 0;
        }

        // Empacota os dados no DTO
        AuthMeResponseDTO response = new AuthMeResponseDTO(
                principal.getUserId(),
                principal.getSessionId(),
                expiresInSeconds
        );

        return ResponseEntity.ok(response);
    }
}