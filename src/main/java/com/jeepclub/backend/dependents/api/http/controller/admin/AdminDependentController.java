package com.jeepclub.backend.dependents.api.http.controller.admin;

import com.jeepclub.backend.dependents.api.http.dto.dependent.DependentResponseDTO;
import com.jeepclub.backend.dependents.core.application.service.dependent.AdminDependentService;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/dependents")
@RequiredArgsConstructor
@Tag(
        name = "Dependents - Admin",
        description = "Consulta administrativa dos dependentes vinculados aos usuários."
)
public class AdminDependentController {

    private final AdminDependentService adminDependentService;

    @GetMapping
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
                .map(DependentResponseDTO::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
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
                DependentResponseDTO.from(
                        adminDependentService.findByUserIdAndId(
                                userId,
                                id
                        )
                )
        );
    }
}
