package com.jeepclub.backend.iam.authentication.api.http.controller.admin;

import com.jeepclub.backend.iam.authentication.api.http.dto.admin.session.AdminSessionResponseDTO;
import com.jeepclub.backend.iam.authentication.core.application.result.admin.session.AdminSessionResult;
import com.jeepclub.backend.iam.authentication.core.application.service.session.AdminSessionService;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(
        value = "/authentication/admin",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Sessions",
        description = "Operações públicas, autenticadas e administrativas para autenticação, consulta e gerenciamento de sessões."
)
public class AdminSessionController {

    private final AdminSessionService adminSessionService;

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('AUTHENTICATION_SESSION_READ')")
    @RequiredPermission("AUTHENTICATION_SESSION_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Listar sessões",
            description = "Retorna as sessões cadastradas no módulo de autenticação.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Sessões retornadas com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminSessionResponseDTO.class)
                            )
                    )
            }
    )
    public ResponseEntity<List<AdminSessionResponseDTO>> findAll() {
        List<AdminSessionResult> results = adminSessionService.findAll();

        return ResponseEntity.ok(AdminSessionResponseDTO.from(results));
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('AUTHENTICATION_SESSION_READ')")
    @RequiredPermission("AUTHENTICATION_SESSION_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Consultar sessão",
            description = "Retorna os dados administrativos de uma sessão específica.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Sessão retornada com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminSessionResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Sessão não encontrada.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<AdminSessionResponseDTO> findById(
            @PathVariable @Positive Long sessionId
    ) {
        AdminSessionResult result = adminSessionService.findById(sessionId);

        return ResponseEntity.ok(AdminSessionResponseDTO.from(result));
    }

    @GetMapping("/users/{userId}/sessions")
    @PreAuthorize("hasAuthority('AUTHENTICATION_SESSION_READ')")
    @RequiredPermission("AUTHENTICATION_SESSION_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Listar sessões de um usuário",
            description = "Retorna as sessões vinculadas a um usuário específico.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Sessões do usuário retornadas com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminSessionResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuário não encontrado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<List<AdminSessionResponseDTO>> findByUserId(
            @PathVariable @Positive Long userId
    ) {
        List<AdminSessionResult> results = adminSessionService.findByUserId(userId);

        return ResponseEntity.ok(AdminSessionResponseDTO.from(results));
    }

    @PatchMapping("/sessions/{sessionId}/logout")
    @PreAuthorize("hasAuthority('AUTHENTICATION_SESSION_LOGOUT')")
    @RequiredPermission("AUTHENTICATION_SESSION_LOGOUT")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Encerrar sessão",
            description = "Encerra administrativamente uma sessão específica.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Sessão encerrada com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminSessionResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Sessão não encontrada.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Sessão já está encerrada ou não pode ser encerrada no estado atual.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<AdminSessionResponseDTO> logout(
            @PathVariable @Positive Long sessionId
    ) {
        AdminSessionResult result = adminSessionService.logout(sessionId);

        return ResponseEntity.ok(AdminSessionResponseDTO.from(result));
    }
}
