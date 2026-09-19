package com.jeepclub.backend.memberships.api.http.controller;

import com.jeepclub.backend.memberships.core.application.service.memberactivationtoken.MemberActivationTokenService;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/membership-applications")
@RequiredArgsConstructor
@Tag(name = "Membership", description = "Solicitação pública de adesão ao clube.")
public class MemberActivationTokenController {

    private final MemberActivationTokenService memberActivationTokenService;

    @GetMapping("/activate")
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(
            summary = "Validar token de ativação",
            description = """
                    Rota pública. Chamada quando o candidato clica no link recebido por e-mail.
                    Valida o token, marca como utilizado e retorna o ID da solicitação confirmada.
                    A partir deste ponto o frontend pode redirecionar para o fluxo de criação de senha.
                    """,
            security = {},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token validado e marcado como utilizado.",
                            content = @Content(mediaType = "application/json", schema = @Schema(type = "object"),
                                    examples = @ExampleObject(value = "{\"message\":\"Token validado com sucesso.\",\"applicationId\":123}"))),
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
    public ResponseEntity<Map<String, Object>> activate(
            @Parameter(description = "Token de ativação recebido por e-mail.", required = true)
            @RequestParam String token
    ) {
        Long applicationId = memberActivationTokenService.validate(token);

        return ResponseEntity.ok(Map.of(
                "message", "Token validado com sucesso.",
                "applicationId", applicationId
        ));
    }
}
