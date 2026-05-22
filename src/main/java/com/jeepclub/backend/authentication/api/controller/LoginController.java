package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dto.login.CompleteRequiredPasswordChangeDTO;
import com.jeepclub.backend.authentication.api.dto.login.LoginRequestDTO;
import com.jeepclub.backend.authentication.api.dto.login.LoginResponseDTO;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.results.login.LoginResult;
import com.jeepclub.backend.authentication.core.application.services.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Login",
        description = "Autenticação de usuários e emissão de tokens."
)
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login")
    @Operation(
            summary = "Autenticar usuário",
            description = """
                    Realiza login com CPF e senha.
                    Quando a senha for definitiva, retorna access token e refresh token.
                    Quando a senha for provisória, retorna um desafio de troca de senha antes da autenticação final.
                    """
    )
    public ResponseEntity<LoginResponseDTO> login(
            @RequestBody @Valid LoginRequestDTO request
    ) {
        LoginResult result = loginService.login(
                request.cpf(),
                request.senha()
        );

        return ResponseEntity.ok(
                LoginResponseDTO.from(result)
        );
    }
    @PostMapping("/login/password-change")
    @Operation(
            summary = "Concluir troca obrigatória de senha",
            description = "Troca a senha provisória por uma senha definitiva e autentica o usuário."
    )
    public ResponseEntity<AuthTokenResponseDTO> completeRequiredPasswordChange(
            @RequestBody @Valid CompleteRequiredPasswordChangeDTO request
    ) {
        AuthTokens tokens = loginService.completeRequiredPasswordChange(
                request.passwordChangeToken(),
                request.newPassword()
        );

        return ResponseEntity.ok(
                new AuthTokenResponseDTO(
                        tokens.refreshToken(),
                        tokens.accessToken(),
                        tokens.expiresInSeconds()
                )
        );
    }
}