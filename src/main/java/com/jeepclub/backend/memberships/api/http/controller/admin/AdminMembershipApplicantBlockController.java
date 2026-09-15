package com.jeepclub.backend.memberships.api.http.controller.admin;

import com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock.AdminMembershipApplicantBlockService;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/membership-applications/blocks")
@RequiredArgsConstructor
@Tag(name = "Membership Admin", description = "Gerenciamento de bloqueios de solicitantes pelo admin.")
public class AdminMembershipApplicantBlockController {

    private final AdminMembershipApplicantBlockService adminMembershipApplicantBlockService;

    @PostMapping("/{cpf}/unblock")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_APPLICANT_UNBLOCK')")
    @RequiredPermission("MEMBERSHIP_MEMBERSHIP_APPLICANT_UNBLOCK")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Desbloquear CPF",
            description = "Encerra o bloqueio ativo do CPF preservando o histórico. Solicitações anteriores não são modificadas.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Bloqueio ativo encerrado."),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão MEMBERSHIP_MEMBERSHIP_APPLICANT_UNBLOCK.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Não há bloqueio ativo para o CPF.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> unblock(
            @Parameter(description = "CPF do solicitante, com ou sem pontuação.", example = "123.456.789-09")
            @PathVariable String cpf,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        adminMembershipApplicantBlockService.unblock(cpf, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
