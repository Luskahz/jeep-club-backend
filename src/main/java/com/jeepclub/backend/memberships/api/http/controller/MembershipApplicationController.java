package com.jeepclub.backend.memberships.api.http.controller;

import com.jeepclub.backend.memberships.api.http.dto.CreateMembershipApplicationRequestDTO;
import com.jeepclub.backend.memberships.api.http.dto.MembershipApplicationSubmissionResponseDTO;
import com.jeepclub.backend.memberships.core.application.result.EnsureMembershipRequestResult;
import com.jeepclub.backend.memberships.core.application.service.membershipapplication.MembershipApplicationService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/membership-applications")
@RequiredArgsConstructor
@Tag(name = "Membership", description = "Solicitação pública de adesão ao clube.")
public class MembershipApplicationController {

    private final MembershipApplicationService membershipApplicationService;

    @PostMapping
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(
            summary = "Solicitar adesão ao clube",
            description = """
                Rota pública. Se já existir uma solicitação aberta para o CPF informado,
                confirma a solicitação existente sem expor os dados previamente persistidos.
                Caso contrário, cria uma nova.
                """,
            security = {},
            responses = {
                    @ApiResponse(responseCode = "201", description = "Solicitação criada.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = MembershipApplicationSubmissionResponseDTO.class))),
                    @ApiResponse(responseCode = "200", description = "Solicitação pendente existente confirmada sem exposição de dados pessoais.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = MembershipApplicationSubmissionResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Dados da solicitação inválidos.",
                            content = @Content(mediaType = "application/problem+json",
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "CPF com bloqueio ativo para novas solicitações.",
                            content = @Content(mediaType = "application/problem+json",
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "CPF ou e-mail já vinculado a usuário, ou e-mail com solicitação pendente.",
                            content = @Content(mediaType = "application/problem+json",
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MembershipApplicationSubmissionResponseDTO> create(
            @Valid @RequestBody CreateMembershipApplicationRequestDTO request
    ) {
        EnsureMembershipRequestResult result = membershipApplicationService.ensure(
                request.name(),
                request.cpf(),
                request.email(),
                request.phoneNumber(),
                request.message()
        );

        MembershipApplicationSubmissionResponseDTO response =
                MembershipApplicationSubmissionResponseDTO.from(result);

        return ResponseEntity
                .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(response);
    }

}
