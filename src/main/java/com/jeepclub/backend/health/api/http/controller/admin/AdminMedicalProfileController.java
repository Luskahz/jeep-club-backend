package com.jeepclub.backend.health.api.http.controller.admin;

import com.jeepclub.backend.health.api.http.dto.MedicalProfileRequest;
import com.jeepclub.backend.health.api.http.dto.MedicalProfileMutationResponse;
import com.jeepclub.backend.health.api.http.dto.MedicalProfilePageResponseSchema;
import com.jeepclub.backend.health.api.http.dto.MedicalProfileResponse;
import com.jeepclub.backend.health.api.http.dto.MedicalProfileSummaryResponse;
import com.jeepclub.backend.health.core.application.service.medicalprofile.AdminMedicalProfileService;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/medical-profiles")
@RequiredArgsConstructor
@Tag(
        name = "Health - Medical Profile Admin",
        description = "Operações administrativas sobre perfis médicos de owners ativos e limpeza de perfis retidos."
)
public class AdminMedicalProfileController {

    private final AdminMedicalProfileService adminMedicalProfileService;

    @GetMapping
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_READ')")
    @RequiredPermission("HEALTH_MEDICAL_PROFILE_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Listar perfis médicos ativos",
            description = """
                    Retorna uma página zero-based de resumos de perfis cujos owners ainda estão ativos.
                    `page` começa em 0; `size` é 20 por padrão e limitado a 50; `sort` usa `id`
                    como ordenação padrão. O retorno mantém o formato direto de `Page` vigente.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Página de perfis médicos retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MedicalProfilePageResponseSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Parâmetros de paginação ou ordenação inválidos.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão HEALTH_MEDICAL_PROFILE_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Conflito de lock durante a consulta de persistência.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Falha não transitória ao acessar a persistência.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Persistência temporariamente indisponível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<PageResponse<MedicalProfileSummaryResponse>> listMedicalProfiles(
            @ParameterObject
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        PageResponse<MedicalProfileSummaryResponse> profiles = PageResponse.from(adminMedicalProfileService
                .listMedicalProfiles(pageable)
                .map(MedicalProfileSummaryResponse::fromDomain));

        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{profileId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_READ')")
    @RequiredPermission("HEALTH_MEDICAL_PROFILE_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Consultar perfil médico por ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Perfil médico retornado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MedicalProfileResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Identificador do perfil inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão HEALTH_MEDICAL_PROFILE_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Perfil ou owner não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "O owner do perfil está inativo ou houve conflito de lock.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Falha não transitória ao acessar a persistência.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Persistência temporariamente indisponível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MedicalProfileResponse> getByProfileId(
            @Parameter(description = "Identificador do perfil médico.", example = "1", required = true)
            @PathVariable Long profileId
    ) {
        var profile = adminMedicalProfileService.getById(
                profileId
        );
        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_READ')")
    @RequiredPermission("HEALTH_MEDICAL_PROFILE_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Consultar perfil médico de um usuário",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Perfil médico retornado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MedicalProfileResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Identificador do usuário inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão HEALTH_MEDICAL_PROFILE_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Perfil ou usuário owner não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "O usuário owner está inativo ou houve conflito de lock.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Falha não transitória ao acessar a persistência.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Persistência temporariamente indisponível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MedicalProfileResponse> getUserMedicalProfile(
            @Parameter(description = "Identificador do usuário proprietário.", example = "10", required = true)
            @PathVariable Long userId
    ) {
        var profile = adminMedicalProfileService.getByOwner(
                MedicalProfileOwnerType.USER,
                userId
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_UPDATE')")
    @RequiredPermission("HEALTH_MEDICAL_PROFILE_UPDATE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Criar ou atualizar perfil médico de um usuário",
            description = "Faz upsert somente para um usuário owner existente e administrativamente ativo.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Confirmação sem reexpor dados clínicos.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MedicalProfileMutationResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Identificador ou dados médicos inválidos.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão HEALTH_MEDICAL_PROFILE_UPDATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Usuário owner não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "O owner está inativo ou houve conflito concorrente no perfil.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Falha não transitória ao persistir o perfil.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Persistência temporariamente indisponível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MedicalProfileMutationResponse> upsertUserMedicalProfile(
            @Parameter(description = "Identificador do usuário proprietário.", example = "10", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody MedicalProfileRequest request
    ) {
        var profile = adminMedicalProfileService.upsertByOwner(
                MedicalProfileOwnerType.USER,
                userId,
                request.toApplicationData()
        );

        return ResponseEntity.ok(MedicalProfileMutationResponse.fromDomain(profile));
    }

    @GetMapping("/dependents/{dependentId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_READ')")
    @RequiredPermission("HEALTH_MEDICAL_PROFILE_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Consultar perfil médico de um dependente",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Perfil médico retornado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MedicalProfileResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Identificador do dependente inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão HEALTH_MEDICAL_PROFILE_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Perfil ou dependente owner não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "O dependente owner está inativo ou houve conflito de lock.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Falha não transitória ao acessar a persistência.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Persistência temporariamente indisponível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MedicalProfileResponse> getDependentMedicalProfile(
            @Parameter(description = "Identificador do dependente proprietário.", example = "10", required = true)
            @PathVariable Long dependentId
    ) {
        var profile = adminMedicalProfileService.getByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @PutMapping("/dependents/{dependentId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_UPDATE')")
    @RequiredPermission("HEALTH_MEDICAL_PROFILE_UPDATE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Criar ou atualizar perfil médico de um dependente",
            description = "Faz upsert somente para um dependente owner existente e ativo.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Confirmação sem reexpor dados clínicos.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MedicalProfileMutationResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Identificador ou dados médicos inválidos.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão HEALTH_MEDICAL_PROFILE_UPDATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Dependente owner não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "O owner está inativo ou houve conflito concorrente no perfil.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Falha não transitória ao persistir o perfil.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Persistência temporariamente indisponível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MedicalProfileMutationResponse> upsertDependentMedicalProfile(
            @Parameter(description = "Identificador do dependente proprietário.", example = "10", required = true)
            @PathVariable Long dependentId,
            @Valid @RequestBody MedicalProfileRequest request
    ) {
        var profile = adminMedicalProfileService.upsertByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId,
                request.toApplicationData()
        );

        return ResponseEntity.ok(MedicalProfileMutationResponse.fromDomain(profile));
    }

    @DeleteMapping("/{profileId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_DELETE')")
    @RequiredPermission("HEALTH_MEDICAL_PROFILE_DELETE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Exclui um perfil médico pelo ID.",
            description = "A limpeza administrativa também pode remover um perfil retido cujo owner ficou inativo ou foi removido; a operação arquiva o histórico antes de remover o registro operacional.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Perfil excluído e histórico preservado."),
                    @ApiResponse(responseCode = "400", description = "Identificador do perfil inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão HEALTH_MEDICAL_PROFILE_DELETE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Perfil médico não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "O perfil já foi removido ou houve conflito concorrente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Falha não transitória ao remover o perfil.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Persistência temporariamente indisponível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> deleteByProfileId(
            @Parameter(description = "Identificador do perfil médico.", example = "1", required = true)
            @PathVariable Long profileId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        adminMedicalProfileService.deleteById(
                profileId,
                principal.getUserId()
        );

        return ResponseEntity.noContent().build();
    }
}
