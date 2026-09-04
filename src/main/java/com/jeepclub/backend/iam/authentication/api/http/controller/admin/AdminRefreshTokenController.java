package com.jeepclub.backend.iam.authentication.api.http.controller.admin;

import com.jeepclub.backend.iam.authentication.api.http.dto.admin.refresh.AdminRefreshTokenResponseDTO;
import com.jeepclub.backend.iam.authentication.core.application.result.admin.refresh.AdminRefreshTokenResult;
import com.jeepclub.backend.iam.authentication.core.application.service.refreshtoken.AdminRefreshTokenService;
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
        name = "Authentication - Refresh Tokens",
        description = "Operações públicas e administrativas para renovação, consulta e gerenciamento de refresh tokens."
)
public class AdminRefreshTokenController {

    private final AdminRefreshTokenService adminRefreshTokenService;

    @GetMapping("/refresh-tokens")
    @PreAuthorize("hasAuthority('AUTHENTICATION_REFRESH_TOKEN_READ')")
    @RequiredPermission("AUTHENTICATION_REFRESH_TOKEN_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Listar refresh tokens",
            description = "Retorna metadados administrativos de refresh tokens cadastrados.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Refresh tokens retornados com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminRefreshTokenResponseDTO.class)
                            )
                    )
            }
    )
    public ResponseEntity<List<AdminRefreshTokenResponseDTO>> findAll() {
        List<AdminRefreshTokenResult> results = adminRefreshTokenService.findAll();

        return ResponseEntity.ok(AdminRefreshTokenResponseDTO.from(results));
    }

    @GetMapping("/refresh-tokens/{refreshTokenId}")
    @PreAuthorize("hasAuthority('AUTHENTICATION_REFRESH_TOKEN_READ')")
    @RequiredPermission("AUTHENTICATION_REFRESH_TOKEN_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Consultar refresh token",
            description = "Retorna metadados administrativos de um refresh token específico.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Refresh token retornado com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminRefreshTokenResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Refresh token não encontrado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<AdminRefreshTokenResponseDTO> findById(
            @PathVariable @Positive Long refreshTokenId
    ) {
        AdminRefreshTokenResult result = adminRefreshTokenService.findById(refreshTokenId);

        return ResponseEntity.ok(AdminRefreshTokenResponseDTO.from(result));
    }

    @GetMapping("/users/{userId}/refresh-tokens")
    @PreAuthorize("hasAuthority('AUTHENTICATION_REFRESH_TOKEN_READ')")
    @RequiredPermission("AUTHENTICATION_REFRESH_TOKEN_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Listar refresh tokens de um usuário",
            description = "Retorna metadados administrativos dos refresh tokens vinculados a um usuário específico.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Refresh tokens do usuário retornados com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminRefreshTokenResponseDTO.class)
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
    public ResponseEntity<List<AdminRefreshTokenResponseDTO>> findByUserId(
            @PathVariable @Positive Long userId
    ) {
        List<AdminRefreshTokenResult> results = adminRefreshTokenService.findByUserId(userId);

        return ResponseEntity.ok(AdminRefreshTokenResponseDTO.from(results));
    }

    @PatchMapping("/refresh-tokens/{refreshTokenId}/revoke")
    @PreAuthorize("hasAuthority('AUTHENTICATION_REFRESH_TOKEN_REVOKE')")
    @RequiredPermission("AUTHENTICATION_REFRESH_TOKEN_REVOKE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Revogar refresh token",
            description = "Revoga administrativamente um refresh token específico.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Refresh token revogado com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminRefreshTokenResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Refresh token não encontrado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Refresh token já está revogado ou não pode ser revogado no estado atual.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<AdminRefreshTokenResponseDTO> revoke(
            @PathVariable @Positive Long refreshTokenId
    ) {
        AdminRefreshTokenResult result = adminRefreshTokenService.revoke(refreshTokenId);

        return ResponseEntity.ok(AdminRefreshTokenResponseDTO.from(result));
    }
}
