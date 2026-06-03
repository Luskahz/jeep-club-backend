package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.token.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dto.sessao.CompleteRequiredPasswordChangeDTO;
import com.jeepclub.backend.authentication.api.dto.sessao.LoginRequestDTO;
import com.jeepclub.backend.authentication.api.dto.sessao.LoginResponseDTO;
import com.jeepclub.backend.authentication.api.dto.sessao.MeResponseDTO;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.results.MeResult;
import com.jeepclub.backend.authentication.core.application.results.login.LoginResult;
import com.jeepclub.backend.authentication.core.application.services.LoginService;
import com.jeepclub.backend.authentication.core.application.services.LogoutService;
import com.jeepclub.backend.authentication.core.application.services.MeService;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/authentication")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Sessions",
        description = "Operações de autenticação, sessão atual e encerramento de sessão."
)
public class SessionController {

    private final LoginService loginService;
    private final LogoutService logoutService;
    private final MeService meService;

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
                AuthTokenResponseDTO.from(tokens)
        );
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Encerrar sessão",
            description = "Encerra a sessão do usuário autenticado com base no token de acesso informado."
    )
    public ResponseEntity<Void> logout(Authentication authentication) {
        UserPrincipal principal = getAuthenticatedPrincipal(authentication);

        logoutService.logout(principal.getUserId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(
            summary = "Consultar sessão autenticada",
            description = "Retorna os dados da sessão do usuário autenticado e suas permissões atuais."
    )
    public ResponseEntity<MeResponseDTO> getMe(Authentication authentication) {
        UserPrincipal principal = getAuthenticatedPrincipal(authentication);

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
                MeResponseDTO.from(result, authorities)
        );
    }

    private UserPrincipal getAuthenticatedPrincipal(Authentication authentication) {
        Objects.requireNonNull(authentication, "authentication cannot be null");

        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("Authenticated principal is not a valid UserPrincipal.");
        }

        return principal;
    }
}