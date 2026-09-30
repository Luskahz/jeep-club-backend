package com.jeepclub.backend.iam.identity.api.http.controller;

import com.jeepclub.backend.iam.identity.api.http.dto.user.UserProfileDataDTO;
import com.jeepclub.backend.iam.identity.api.http.dto.user.UserProfileResponseDTO;
import com.jeepclub.backend.iam.identity.core.application.service.user.UserProfileService;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping(value = "/identity/me/profile", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Identity - Users")
@ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
@ApiResponse(responseCode = "404", description = "User não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
public class UserProfileController {
    private final UserProfileService profiles;

    @GetMapping
    @SwaggerOperationGroup(value = "Rotas autenticadas", order = 20)
    @Operation(summary = "Consultar meu perfil complementar", description = "Usa exclusivamente o principal autenticado. "
            + "Perfil ainda não preenchido retorna 200, blocos null e profileCompletionPending=true, sem criar registro.")
    @ApiResponse(responseCode = "200", description = "Perfil complementar e pendência derivados dos dados.", content = @Content(schema = @Schema(implementation = UserProfileResponseDTO.class)))
    public UserProfileResponseDTO get(@AuthenticationPrincipal UserPrincipal principal) {
        return UserProfileResponseDTO.from(profiles.get(principal.getUserId()));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @SwaggerOperationGroup(value = "Rotas autenticadas", order = 20)
    @Operation(summary = "Preencher ou substituir meu perfil complementar", description = "Upsert integral, limitado ao principal autenticado. "
            + "Omissão/null/branco remove dados; {} limpa o perfil. Aceita preenchimento parcial. "
            + "Não altera dados básicos de PATCH /identity/me nem o estado administrativo. IDs e campos desconhecidos são rejeitados.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = UserProfileDataDTO.class))))
    @ApiResponse(responseCode = "200", description = "Perfil criado ou substituído.", content = @Content(schema = @Schema(implementation = UserProfileResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Campo desconhecido, tipo, tamanho, CEP ou UF inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    public UserProfileResponseDTO replace(@AuthenticationPrincipal UserPrincipal principal, @RequestBody JsonNode body) {
        var data = UserProfileDataDTO.read(body);
        return UserProfileResponseDTO.from(profiles.replace(principal.getUserId(),
                data.workProfile() == null ? null : data.workProfile().toDomain(),
                data.address() == null ? null : data.address().toDomain()));
    }
}
