package com.jeepclub.backend.iam.identity.api.http.controller;

import com.jeepclub.backend.iam.identity.api.http.dto.user.CurrentUserResponseDTO;
import com.jeepclub.backend.iam.identity.api.http.dto.user.UpdateCurrentUserEmailRequestDTO;
import com.jeepclub.backend.iam.identity.api.http.dto.user.UpdateCurrentUserProfileRequestDTO;
import com.jeepclub.backend.iam.identity.api.http.dto.user.UpdateCurrentUserPhotoRequestDTO;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException;
import com.jeepclub.backend.iam.identity.core.application.service.user.CurrentUserProfileService;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping(value = "/identity", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Identity - Users", description = "Cadastro e consulta dos dados cadastrais dos usuários.")
public class CurrentUserController {
    private final UserQuery userQuery;
    private final CurrentUserProfileService currentUserProfileService;

    @GetMapping("/me")
    @SwaggerOperationGroup(value = "Rotas autenticadas", order = 20)
    @Operation(
            summary = "Consultar meus dados cadastrais",
            description = "Retorna exclusivamente os dados pertencentes ao User do módulo Identity.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dados cadastrais retornados.",
                            content = @Content(schema = @Schema(implementation = CurrentUserResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "User não encontrado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<CurrentUserResponseDTO> getMe(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserDetails user = userQuery.findById(principal.getUserId())
                .orElseThrow(() -> new UserNotFoundException(principal.getUserId()));
        return ResponseEntity.ok(CurrentUserResponseDTO.from(user));
    }

    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SwaggerOperationGroup(value = "Rotas autenticadas", order = 20)
    @Operation(
            summary = "Atualizar meus dados cadastrais",
            description = "Edita somente name, birthDate, rg, phoneNumber e profilePhotoStorageKey do principal autenticado. "
                    + "Campo omitido preserva o valor; null limpa opcionais, mas name não aceita null/branco. "
                    + "Objeto vazio não altera updatedAt. CPF, IDs, status, roles, permissions, timestamps e campos desconhecidos "
                    + "são rejeitados. E-mail tem fluxo próprio em /identity/me/email. "
                    + "Foto exige storageKey existente de /media/images; não aceita URL arbitrária. Remover a associação não apaga o arquivo.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateCurrentUserProfileRequestDTO.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cadastro atualizado, ou preservado para objeto vazio.",
                            content = @Content(schema = @Schema(implementation = CurrentUserResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Campo proibido/desconhecido, tipo inválido, nome ausente em campo enviado ou referência de imagem inválida.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Usuário ou imagem não encontrado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "RG já utilizado por outro usuário.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<CurrentUserResponseDTO> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody JsonNode request
    ) {
        return ResponseEntity.ok(CurrentUserResponseDTO.from(currentUserProfileService.updateProfile(
                principal.getUserId(), UpdateCurrentUserProfileRequestDTO.read(request))));
    }

    @PatchMapping(value = "/me/email", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SwaggerOperationGroup(value = "Rotas autenticadas", order = 20)
    @Operation(
            summary = "Cadastrar ou alterar meu e-mail",
            description = "Atualiza exclusivamente o e-mail do User autenticado, com normalização e verificação de unicidade.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "E-mail atualizado.",
                            content = @Content(schema = @Schema(implementation = CurrentUserResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "E-mail ausente ou inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "User não encontrado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "E-mail já utilizado por outro User.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<CurrentUserResponseDTO> updateEmail(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateCurrentUserEmailRequestDTO request
    ) {
        return ResponseEntity.ok(CurrentUserResponseDTO.from(
                currentUserProfileService.updateEmail(principal.getUserId(), request.email())
        ));
    }

    @PatchMapping(value = "/me/photo", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SwaggerOperationGroup(value = "Rotas autenticadas", order = 20)
    @Operation(summary = "Associar foto de perfil", description = "Recebe storageKey de POST /media/images, confirma que o objeto existe e devolve a chave no perfil. null desassocia sem excluir o objeto do storage.")
    public ResponseEntity<CurrentUserResponseDTO> updatePhoto(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateCurrentUserPhotoRequestDTO request
    ) {
        return ResponseEntity.ok(CurrentUserResponseDTO.from(
                currentUserProfileService.updateProfilePhoto(principal.getUserId(), request.storageKey())
        ));
    }
}
