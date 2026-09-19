package com.jeepclub.backend.memberships.api.http.controller;

import com.jeepclub.backend.memberships.core.application.service.memberactivationtoken.MemberActivationTokenService;
import com.jeepclub.backend.memberships.api.http.dto.CompleteMemberActivationRequestDTO;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/membership-applications")
@RequiredArgsConstructor
@Validated
@Tag(name = "Membership", description = "Solicitação pública de adesão ao clube.")
public class MemberActivationTokenController {

    private final MemberActivationTokenService memberActivationTokenService;

    @GetMapping("/activate")
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(
            summary = "Validar token de ativação",
            description = """
                    Rota pública. Chamada quando o candidato clica no link recebido por e-mail.
                    Valida o token sem consumir a credencial nem alterar o banco.
                    A conclusão do primeiro acesso é feita exclusivamente pelo POST correspondente.
                    """,
            security = {},
            responses = {
                    @ApiResponse(responseCode = "204", description = "Token válido; nenhuma alteração persistida."),
                    @ApiResponse(responseCode = "400", description = "Parâmetro token ausente.",
                            content = @Content(mediaType = "application/problem+json",
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Token de ativação não encontrado.",
                            content = @Content(mediaType = "application/problem+json",
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Token de ativação já utilizado.",
                            content = @Content(mediaType = "application/problem+json",
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "410", description = "Token de ativação expirado.",
                            content = @Content(mediaType = "application/problem+json",
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> validate(
            @Parameter(description = "Token de ativação recebido por e-mail.", required = true)
            @RequestParam @NotBlank String token
    ) {
        memberActivationTokenService.validate(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/activate", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(
            summary = "Concluir primeiro acesso por token de ativação",
            description = "Define a senha definitiva em Authentication e, somente após sucesso, consome o token e conclui a MembershipApplication.",
            security = {},
            responses = {
                    @ApiResponse(responseCode = "204", description = "Primeiro acesso concluído."),
                    @ApiResponse(responseCode = "400", description = "Body ou senha inválidos.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Token ou recurso vinculado não encontrado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Token usado ou estado incompatível.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "410", description = "Token expirado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> complete(
            @Valid @RequestBody CompleteMemberActivationRequestDTO request
    ) {
        memberActivationTokenService.complete(request.token(), request.newPassword());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
