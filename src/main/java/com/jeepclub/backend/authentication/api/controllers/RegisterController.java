package com.jeepclub.backend.authentication.api.controllers;

import com.jeepclub.backend.authentication.api.dtos.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dtos.UserRegisterRequest;
import com.jeepclub.backend.authentication.api.dtos.UserRegisterResponse;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.services.LoginService;
import com.jeepclub.backend.authentication.core.application.services.RegisterService;
import com.jeepclub.backend.authentication.core.domain.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class RegisterController {

    private final RegisterService registerService;
    private final LoginService loginService;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody @Valid @NotNull UserRegisterRequest request) {
        try {
            User newUser = registerService.registerUser(
                    request.name(),
                    request.birthData(),
                    request.email(),
                    request.cpf(),
                    request.rg(),
                    request.password(),
                    request.phoneNumber()
            );

            AuthTokens tokens = loginService.login(
                    newUser.getCpf(),
                    request.password()
            );
            AuthTokenResponseDTO response = new AuthTokenResponseDTO(
                    tokens.refreshToken(),
                    tokens.accessToken(),
                    tokens.expiresInSeconds()
            );
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}