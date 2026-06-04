package com.jeepclub.backend.authentication.api.controller.refresh;

import com.jeepclub.backend.authentication.api.dto.token.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dto.token.RefreshTokenRequestDTO;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.services.RefreshTokenService;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        value = "/authentication",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Refresh Tokens",
        description = "Rotação e renovação de tokens de autenticação."
)
public class RefreshTokenController {

    private final RefreshTokenService refreshTokenService;

    @PostMapping(
            value = "/refresh",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(
            summary = "Renovar tokens",
            description = """
                    Gera um novo access token e um novo refresh token a partir de um refresh token válido.
                    O refresh token informado deve estar válido, ativo e associado a uma sessão existente.
                    """,
            security = {},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Tokens renovados com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AuthTokenResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Requisição inválida ou refresh token inconsistente.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Refresh token inválido, expirado, revogado ou vinculado a uma sessão inválida.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<AuthTokenResponseDTO> refresh(
            @RequestBody @Valid RefreshTokenRequestDTO request
    ) {
        AuthTokens tokens = refreshTokenService.refresh(
                request.refreshToken()
        );

        return ResponseEntity.ok(
                AuthTokenResponseDTO.from(tokens)
        );
    }
}