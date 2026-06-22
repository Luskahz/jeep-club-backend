package com.jeepclub.backend.authentication.api.controller.user;

import com.jeepclub.backend.authentication.api.dto.token.AuthTokenResponseDTO;
import com.jeepclub.backend.authentication.api.dto.user.UserRegistrationRequestDTO;
import com.jeepclub.backend.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.authentication.core.application.service.RegisterAndAuthenticateService;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        value = "/authentication",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - User Registration",
        description = "Registro público de usuários e emissão inicial de tokens."
)
public class UserController {

    private final RegisterAndAuthenticateService registrationService;

    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(
            summary = "Registrar usuário",
            description = """
                    Cria uma nova conta de usuário a partir dos dados cadastrais informados.
                    Após o registro, autentica o usuário criado e retorna access token e refresh token.
                    """,
            security = {},
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Usuário registrado e autenticado com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AuthTokenResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Requisição inválida ou dados cadastrais inconsistentes.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Já existe usuário cadastrado com os dados únicos informados.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<AuthTokenResponseDTO> register(
            @RequestBody @Valid UserRegistrationRequestDTO request
    ) {
        AuthTokens tokens = registrationService.registerAndAuthenticate(
                request.name(),
                request.birthDate(),
                request.email(),
                request.cpf(),
                request.rg(),
                request.password(),
                request.phoneNumber()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthTokenResponseDTO.from(tokens));
    }
}
