package com.jeepclub.backend.identity.api.http.controller;

import com.jeepclub.backend.identity.api.http.dto.user.UserAuthenticationTokenResponseDTO;
import com.jeepclub.backend.identity.api.http.dto.user.UserRegistrationRequestDTO;
import com.jeepclub.backend.identity.api.module.UserAuthenticationTokens;
import com.jeepclub.backend.identity.api.module.UserRegistration;
import com.jeepclub.backend.identity.api.module.UserRegistrationData;
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

import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping(value = "/identity", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Identity - Users", description = "Cadastro e consulta dos dados cadastrais dos usuários.")
public class UserRegistrationController {
    private final UserRegistration userRegistration;
    private final Clock clock;

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(
            summary = "Registrar usuário",
            description = "Cria o User, provisiona sua autenticação e devolve os tokens iniciais.",
            security = {},
            responses = {
                    @ApiResponse(responseCode = "201", description = "Usuário registrado e autenticado.",
                            content = @Content(schema = @Schema(
                                    implementation = UserAuthenticationTokenResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos.",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Dados cadastrais já utilizados.",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<UserAuthenticationTokenResponseDTO> register(
            @RequestBody @Valid UserRegistrationRequestDTO request
    ) {
        UserAuthenticationTokens tokens = userRegistration.registerAndAuthenticate(
                new UserRegistrationData(
                        request.name(), request.birthDate(), request.email(), request.cpf(), request.rg(),
                        request.phoneNumber(), null, Instant.now(clock)
                ),
                request.password()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserAuthenticationTokenResponseDTO.from(tokens));
    }
}
