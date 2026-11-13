package com.jeepclub.backend.dependents.api.http.controller.admin;

import com.jeepclub.backend.dependents.api.http.dto.dependent.AdminDependentFilterDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.AdminDependentPageResponseSchema;
import com.jeepclub.backend.dependents.api.http.dto.dependent.AdminDependentResponseDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.DependentResponseDTO;
import com.jeepclub.backend.dependents.core.application.query.AdminDependentField;
import com.jeepclub.backend.dependents.core.application.service.dependent.AdminDependentService;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Dependents - Admin",
        description = "Consulta e gestão administrativa dos dependentes vinculados aos usuários."
)
public class AdminDependentController {

    private final AdminDependentService adminDependentService;
    private final com.jeepclub.backend.dependents.core.application.service.dependent.DependentService lifecycle;

    @GetMapping("/admin/dependents")
    @ApiResponse(responseCode = "400", description = "Filtros, fields, sort ou paginação inválidos.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_READ')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_READ")
    @Operation(
            summary = "Listar dependentes administrativamente",
            description = "Página zero-based de dependentes operacionais (ACTIVE, INACTIVE e ARCHIVED); size default 20 e máximo global 50. q usa OR entre nome do dependente, nome do titular (sem limite de cardinalidade) e, quando o termo contiver apenas dígitos e pontuação de documento/telefone, CPF e telefone. name, cpf, userId, relationshipType e status combinam com AND antes da página/count. Texto literal case-insensitive com trim: %, _ e ! não são curingas. fields opcional seleciona colunas e omite campos não pedidos; default preserva todos os campos administrativos sem expor photoStorageKey. Sort permitido: id,name,cpf,birthDate,relationshipType,phoneNumber,userId,status,createdAt,updatedAt,archivedAt; id asc é o desempate.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Página de dependentes retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AdminDependentPageResponseSchema.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão DEPENDENTS_DEPENDENT_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<PageResponse<?>> listDependents(
            @Valid @ParameterObject @ModelAttribute AdminDependentFilterDTO filters,
            @Parameter(description = "ID,NAME,CPF,BIRTH_DATE,RELATIONSHIP_TYPE,PHONE_NUMBER,USER_ID,OWNER_NAME,STATUS,CREATED_AT,UPDATED_AT,ARCHIVED_AT,PHOTO_URL. Omitido/vazio preserva todos os campos; OWNER_NAME não expõe userId auxiliar.")
            @RequestParam(required = false) Set<AdminDependentField> fields,
            @Parameter(hidden = true) @RequestParam(required = false, name = "page") @Min(0) Integer requestedPage,
            @Parameter(hidden = true) @RequestParam(required = false, name = "size") @Min(1) Integer requestedSize,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        boolean complete = fields == null || fields.isEmpty();
        Set<AdminDependentField> selected = complete ? EnumSet.allOf(AdminDependentField.class) : fields;
        var page = adminDependentService.search(filters.toFilter(), selected, pageable);
        return ResponseEntity.ok(complete
                ? PageResponse.from(page.map(AdminDependentResponseDTO::from))
                : PageResponse.from(page.map(row -> AdminDependentResponseDTO.selected(row, selected))));
    }

    @GetMapping("/admin/dependents/{id}")
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_READ')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_READ")
    @Operation(
            summary = "Consultar dependente administrativamente por ID",
            description = "Retorna os dados operacionais de um dependente (ACTIVE, INACTIVE ou ARCHIVED) por ID, acompanhado do nome do titular.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dependente operacional retornado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AdminDependentResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Formato do identificador inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão DEPENDENTS_DEPENDENT_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Dependente não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<AdminDependentResponseDTO> getDependentById(
            @Parameter(description = "Identificador do dependente.", required = true, example = "10")
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(AdminDependentResponseDTO.from(adminDependentService.findAdminById(id)));
    }

    @GetMapping("/users/{userId}/dependents")
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_READ')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_READ")
    @Operation(
            summary = "Listar dependentes de um usuário",
            description = "Lista os dependentes operacionais vinculados ao usuário informado, incluindo ativos e desabilitados. Registros históricos removidos não fazem parte da resposta.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dependentes operacionais retornados.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = DependentResponseDTO.class)))),
                    @ApiResponse(responseCode = "400", description = "Formato do identificador do usuário inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão DEPENDENTS_DEPENDENT_READ.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<List<DependentResponseDTO>> getDependentsByUserId(
            @Parameter(
                    description = "Identificador do usuário titular.",
                    required = true
            )
            @PathVariable Long userId
    ) {
        List<DependentResponseDTO> response = adminDependentService
                .findAllByUserId(userId)
                .stream()
                .map(DependentResponseDTO::fromAdmin)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}/dependents/{id}")
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_READ')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_READ")
    @Operation(
            summary = "Consultar dependente de um usuário",
            description = "Consulta um dependente operacional específico vinculado ao usuário informado, ativo ou desabilitado. Registros históricos removidos não fazem parte da resposta.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dependente operacional retornado.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = DependentResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Formato dos identificadores inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão DEPENDENTS_DEPENDENT_READ.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Dependente não encontrado ou não pertence ao usuário informado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<DependentResponseDTO> getDependentByUserAndId(
            @Parameter(
                    description = "Identificador do usuário titular.",
                    required = true
            )
            @PathVariable Long userId,

            @Parameter(
                    description = "Identificador do dependente.",
                    required = true
            )
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                DependentResponseDTO.fromAdmin(
                        adminDependentService.findByUserIdAndId(
                                userId,
                                id
                        )
                )
        );
    }

    @PatchMapping("/users/{userId}/dependents/{id}/disable")
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_DEACTIVATE')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_DEACTIVATE")
    @Operation(summary="disable dependente administrativamente", description="HTTP 409 DEPENDENT_LIFECYCLE_CONFLICT para participacao operacional em Event nao encerrado; sem cancelamento ou desassociacao. Historico e permitido.")
    @ApiResponse(responseCode="409",description="Transição incompatível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DependentResponseDTO> disable(@PathVariable @jakarta.validation.constraints.Positive Long userId,
            @PathVariable @jakarta.validation.constraints.Positive Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.jeepclub.backend.platform.security.principal.UserPrincipal principal) {
        return ResponseEntity.ok(DependentResponseDTO.fromAdmin(lifecycle.disable(id,userId,principal.getUserId())));
    }

    @PatchMapping("/users/{userId}/dependents/{id}/enable")
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_ACTIVATE')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_ACTIVATE")
    @Operation(summary="enable dependente administrativamente", description="Preserva titular e histórico; unarchive retorna a INACTIVE.")
    @ApiResponse(responseCode="409",description="Transição incompatível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DependentResponseDTO> enable(@PathVariable @jakarta.validation.constraints.Positive Long userId,
            @PathVariable @jakarta.validation.constraints.Positive Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.jeepclub.backend.platform.security.principal.UserPrincipal principal) {
        return ResponseEntity.ok(DependentResponseDTO.fromAdmin(lifecycle.enable(id,userId,principal.getUserId())));
    }

    @PatchMapping("/users/{userId}/dependents/{id}/archive")
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_ARCHIVE')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_ARCHIVE")
    @Operation(summary="archive dependente administrativamente", description="HTTP 409 DEPENDENT_LIFECYCLE_CONFLICT para participacao operacional em Event nao encerrado; sem cancelamento ou desassociacao. Historico e permitido.")
    @ApiResponse(responseCode="409",description="Transição incompatível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DependentResponseDTO> archive(@PathVariable @jakarta.validation.constraints.Positive Long userId,
            @PathVariable @jakarta.validation.constraints.Positive Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.jeepclub.backend.platform.security.principal.UserPrincipal principal) {
        return ResponseEntity.ok(DependentResponseDTO.fromAdmin(lifecycle.archive(id,userId,principal.getUserId())));
    }

    @PatchMapping("/users/{userId}/dependents/{id}/unarchive")
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_UNARCHIVE')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_UNARCHIVE")
    @Operation(summary="unarchive dependente administrativamente", description="Preserva titular e histórico; unarchive retorna a INACTIVE.")
    @ApiResponse(responseCode="409",description="Transição incompatível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DependentResponseDTO> unarchive(@PathVariable @jakarta.validation.constraints.Positive Long userId,
            @PathVariable @jakarta.validation.constraints.Positive Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.jeepclub.backend.platform.security.principal.UserPrincipal principal) {
        return ResponseEntity.ok(DependentResponseDTO.fromAdmin(lifecycle.unarchive(id,userId,principal.getUserId())));
    }

    @DeleteMapping("/users/{userId}/dependents/{id}")
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_DELETE')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_DELETE")
    @Operation(summary="Excluir dependente administrativamente", description="Snapshot registra o administrador real. Health/Event retidos impedem a exclusão.")
    @ApiResponse(responseCode="204",description="Excluído com histórico.")
    @ApiResponse(responseCode="409",description="Referências impeditivas.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<Void> delete(@PathVariable @jakarta.validation.constraints.Positive Long userId,
            @PathVariable @jakarta.validation.constraints.Positive Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.jeepclub.backend.platform.security.principal.UserPrincipal principal) {
        lifecycle.delete(id,userId,principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
