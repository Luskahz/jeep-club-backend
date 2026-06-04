package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.sessao.CompleteRequiredPasswordChangeDTO;
import com.jeepclub.backend.authentication.api.dto.sessao.LoginRequestDTO;
import com.jeepclub.backend.authentication.api.dto.sessao.LoginResponseDTO;
import com.jeepclub.backend.authentication.api.dto.sessao.MeResponseDTO;
import com.jeepclub.backend.authentication.api.dto.token.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.results.MeResult;
import com.jeepclub.backend.authentication.core.application.results.login.LoginResult;
import com.jeepclub.backend.authentication.core.application.services.SessionService;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(
        value = "/authentication",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Sessions",
        description = "Operações de autenticação, sessão atual e encerramento de sessão."
)
public class SessionController {

    private final SessionService sessionService;

    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(
            summary = "Autenticar usuário",
            description = """
                    Realiza login com CPF e senha.
                    Quando a senha for definitiva, retorna access token e refresh token.
                    Quando a senha for provisória, retorna um desafio de troca de senha antes da autenticação final.
                    """,
            security = {},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Login processado com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = LoginResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Requisição inválida ou credenciais em formato inconsistente.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Credenciais inválidas.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<LoginResponseDTO> login(
            @RequestBody @Valid LoginRequestDTO request
    ) {
        LoginResult result = sessionService.login(
                request.cpf(),
                request.senha()
        );

        return ResponseEntity.ok(
                LoginResponseDTO.from(result)
        );
    }

    @PostMapping(
            value = "/login/password-change",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(
            summary = "Concluir troca obrigatória de senha",
            description = """
                    Troca a senha provisória por uma senha definitiva.
                    Após a troca, autentica o usuário e retorna access token e refresh token.
                    """,
            security = {},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Senha alterada e autenticação concluída com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AuthTokenResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Requisição inválida, token de troca inválido ou nova senha inconsistente.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Token de troca expirado, inválido ou não autorizado para concluir o fluxo.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<AuthTokenResponseDTO> completeRequiredPasswordChange(
            @RequestBody @Valid CompleteRequiredPasswordChangeDTO request
    ) {
        AuthTokens tokens = sessionService.completeRequiredPasswordChange(
                request.passwordChangeToken(),
                request.newPassword()
        );

        return ResponseEntity.ok(
                AuthTokenResponseDTO.from(tokens)
        );
    }

    @PostMapping("/logout")
    @SwaggerOperationGroup(value = "Rotas autenticadas", order = 20)
    @Operation(
            summary = "Encerrar sessão",
            description = "Encerra a sessão do usuário autenticado com base no token de acesso informado.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Sessão encerrada com sucesso.",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Usuário não autenticado ou token de acesso inválido.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<Void> logout(Authentication authentication) {
        UserPrincipal principal = getAuthenticatedPrincipal(authentication);

        sessionService.logout(principal.getUserId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @SwaggerOperationGroup(value = "Rotas autenticadas", order = 20)
    @Operation(
            summary = "Consultar sessão autenticada",
            description = "Retorna os dados da sessão do usuário autenticado e suas permissões atuais.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Sessão autenticada retornada com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MeResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Usuário não autenticado ou token de acesso inválido.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<MeResponseDTO> getMe(Authentication authentication) {
        UserPrincipal principal = getAuthenticatedPrincipal(authentication);

        MeResult result = sessionService.me(
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