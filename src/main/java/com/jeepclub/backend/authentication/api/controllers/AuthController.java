package com.jeepclub.backend.authentication.api.controllers;

import com.jeepclub.backend.authentication.api.dtos.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dtos.login.UserLoginRequest;
import com.jeepclub.backend.authentication.api.dtos.UserRegisterRequest;
import com.jeepclub.backend.authentication.api.dtos.UserRegisterResponse;
import com.jeepclub.backend.authentication.core.services.AuthService;
import com.jeepclub.backend.authentication.core.services.AuthTokens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserRegisterResponse> register(@RequestBody UserRegisterRequest request) {
        var newUser = authService.registerUser(
                request.name(),
                request.birthData(),
                request.email(),
                request.cpf(),
                request.rg(),
                request.password(),
                request.phoneNumber());

        return ResponseEntity.status(HttpStatus.CREATED).body(UserRegisterResponse.fromDomain(newUser));
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
}
