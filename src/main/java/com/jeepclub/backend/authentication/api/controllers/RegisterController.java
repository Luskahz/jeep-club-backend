package com.jeepclub.backend.authentication.api.controllers;

import com.jeepclub.backend.authentication.api.dtos.UserRegisterRequest;
import com.jeepclub.backend.authentication.api.dtos.UserRegisterResponse;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Melhorar esse docs @Daniel
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class RegisterController {

    private final AuthService authService;

    /**
     * Melhorar esse Docs @Daniel
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
}
