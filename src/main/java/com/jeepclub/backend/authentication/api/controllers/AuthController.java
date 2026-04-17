package com.jeepclub.backend.authentication.api.controllers;

import com.jeepclub.backend.authentication.api.dtos.UserMeResponse;
import com.jeepclub.backend.authentication.api.dtos.UserRegisterRequest;
import com.jeepclub.backend.authentication.api.dtos.UserRegisterResponse;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.services.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



/**
 * Porta Principal / Adaptador de Entrada Primária (Primary Inbound Adapter).
 * Única e exclusiva camada a se preocupar com transporte da web (JSON, HTTP
 * Headers, Códigos de Status).
 */

@RestController
@RequestMapping("/users")
public class AuthController {

    private final AuthService authService;

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
     * Endpoint para obter os dados da sessão/token do usuário logado.
     */
    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getMe(@RequestHeader("Authorization") String authorizationHeader) {

        // Remove o prefixo "Bearer " do token para pegar apenas a chave
        String token = authorizationHeader.replace("Bearer ", "");

        // Delegamos para o AuthService decodificar o token e extrair as informações
        UserMeResponse response = authService.getUserSessionInfoFromToken(token);

        return ResponseEntity.ok(response);
    }
}