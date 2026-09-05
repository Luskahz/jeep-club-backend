package com.jeepclub.backend.iam.authentication.api.http.controller;

import com.jeepclub.backend.iam.authentication.api.http.dto.session.CompleteRequiredPasswordChangeDTO;
import com.jeepclub.backend.iam.authentication.api.http.dto.session.LoginRequestDTO;
import com.jeepclub.backend.iam.authentication.api.http.dto.session.LoginResponseDTO;
import com.jeepclub.backend.iam.authentication.api.http.dto.session.MeResponseDTO;
import com.jeepclub.backend.iam.authentication.api.http.dto.token.AuthTokenResponseDTO;
import com.jeepclub.backend.iam.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.iam.authentication.core.application.result.MeResult;
import com.jeepclub.backend.iam.authentication.core.application.result.login.LoginResult;
import com.jeepclub.backend.iam.authentication.core.application.service.session.SessionService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        value = "/authentication",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Sessions",
        description = "Operações públicas, autenticadas e administrativas para autenticação, consulta e gerenciamento de sessões."
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
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        sessionService.logout(principal.getUserId(), principal.getSessionId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @SwaggerOperationGroup(value = "Rotas autenticadas", order = 20)
    @Operation(
            summary = "Consultar sessão autenticada",
            description = "Retorna somente os dados técnicos da sessão do usuário autenticado.",
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
    public ResponseEntity<MeResponseDTO> getMe(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MeResult result = sessionService.getCurrentSession(
                principal.getUserId(),
                principal.getSessionId(),
                principal.getAccessTokenExpiresAt()
        );

        return ResponseEntity.ok(
                MeResponseDTO.from(result)
        );
    }
}
