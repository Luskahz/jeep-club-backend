package com.jeepclub.backend.memberships.api.http.controller.admin;

import com.jeepclub.backend.memberships.api.http.dto.AccessLinkApprovalResponseDTO;
import com.jeepclub.backend.memberships.api.http.dto.BlockMembershipApplicantRequestDTO;
import com.jeepclub.backend.memberships.api.http.dto.MembershipApplicationResponseDTO;
import com.jeepclub.backend.memberships.api.http.dto.MembershipApplicationPageResponseSchema;
import com.jeepclub.backend.memberships.api.http.dto.RejectMembershipRequestDTO;
import com.jeepclub.backend.memberships.api.http.dto.TemporaryPasswordApprovalResponseDTO;
import com.jeepclub.backend.memberships.core.application.service.membershipapplication.AdminMembershipApplicationService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/membership-applications")
@RequiredArgsConstructor
@Tag(name = "Membership Admin", description = "Gerenciamento de solicitações de adesão pelo admin.")
public class AdminMembershipApplicationController {

    private final AdminMembershipApplicationService adminMembershipApplicationService;

    @GetMapping
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_READ')")
    @RequiredPermission("MEMBERSHIP_MEMBERSHIP_REQUEST_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Listar solicitações",
            description = """
                    Lista solicitações de adesão, opcionalmente filtradas por status.
                    A paginação é zero-based: page e size têm os defaults globais do Spring Data,
                    com size 20 nesta rota. A ordenação padrão é requestedAt em ordem decrescente.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Página de solicitações retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MembershipApplicationPageResponseSchema.class))),
                    @ApiResponse(responseCode = "400", description = "Status, paginação ou ordenação inválidos.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão MEMBERSHIP_MEMBERSHIP_REQUEST_READ.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<PageResponse<MembershipApplicationResponseDTO>> list(
            @Parameter(description = "Filtra solicitações pelo estado atual.", example = "PENDING")
            @RequestParam(required = false) MembershipApplicationStatus status,
            @ParameterObject
            @PageableDefault(
                    size = 20,
                    sort = "requestedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        PageResponse<MembershipApplicationResponseDTO> response = PageResponse.from(
                (status != null
                        ? adminMembershipApplicationService.listByStatus(status, pageable)
                        : adminMembershipApplicationService.listAll(pageable))
                        .map(MembershipApplicationResponseDTO::fromDomain));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_READ')")
    @RequiredPermission("MEMBERSHIP_MEMBERSHIP_REQUEST_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Buscar solicitação por ID",
            description = "Retorna todos os campos da solicitação.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Solicitação retornada.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MembershipApplicationResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Identificador da solicitação inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão MEMBERSHIP_MEMBERSHIP_REQUEST_READ.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MembershipApplicationResponseDTO> getById(
            @Parameter(description = "Identificador da solicitação.", example = "123")
            @PathVariable Long id
    ) {
        MembershipApplicationResponseDTO response = MembershipApplicationResponseDTO.fromDomain(
                adminMembershipApplicationService.findById(id)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve/temporary-password")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE')")
    @RequiredPermission("MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Aprovar solicitação com senha temporária",
            description = "Cria o usuário com PENDING_FIRST_ACCESS e retorna a senha temporária uma única vez para o administrador.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Solicitação aprovada e usuário criado.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = TemporaryPasswordApprovalResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Identificador da solicitação inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Solicitação não está pendente para aprovação.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<TemporaryPasswordApprovalResponseDTO> approveWithTemporaryPassword(
            @Parameter(description = "Identificador da solicitação pendente.", example = "123")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var result = adminMembershipApplicationService.approveWithTemporaryPassword(
                id,
                principal.getUserId()
        );

        return ResponseEntity.ok(TemporaryPasswordApprovalResponseDTO.from(result));
    }

    @PostMapping("/{id}/approve/access-link")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE')")
    @RequiredPermission("MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Aprovar solicitação com link de acesso",
            description = "Cria o usuário com PENDING_FIRST_ACCESS, emite o token próprio de Membership e envia o convite por e-mail sem retornar o segredo.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Solicitação aprovada e usuário criado.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AccessLinkApprovalResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Identificador da solicitação inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Solicitação não está pendente ou o User não possui e-mail.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<AccessLinkApprovalResponseDTO> approveWithAccessLink(
            @Parameter(description = "Identificador da solicitação pendente.", example = "123")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var result = adminMembershipApplicationService.approveWithAccessLink(
                id,
                principal.getUserId()
        );

        return ResponseEntity.ok(AccessLinkApprovalResponseDTO.from(result));
    }

    @PostMapping("/{id}/activation-link/resend")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE')")
    @RequiredPermission("MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Reenviar convite de ativação",
            description = "Invalida convites ativos, emite um novo token sem recriar User ou AuthenticationAccount e envia ao e-mail atual do User.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Novo convite emitido e enviado."),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem permissão de aprovação.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Solicitação incompatível ou User sem e-mail.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> resendActivationLink(@PathVariable Long id) {
        adminMembershipApplicationService.resendActivationLink(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT')")
    @RequiredPermission("MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Rejeitar solicitação",
            description = "Motivo é opcional. Quando informado, é incluído na notificação ao candidato.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Solicitação rejeitada."),
                    @ApiResponse(responseCode = "400", description = "Identificador da solicitação inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Solicitação não está pendente para rejeição.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> reject(
            @Parameter(description = "Identificador da solicitação pendente.", example = "123")
            @PathVariable Long id,
            @RequestBody(required = false) RejectMembershipRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String reason = request != null ? request.reason() : null;
        adminMembershipApplicationService.reject(id, principal.getUserId(), reason);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{id}/reject-and-block")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_APPLICANT_BLOCK')")
    @RequiredPermission("MEMBERSHIP_MEMBERSHIP_APPLICANT_BLOCK")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Rejeitar solicitação e bloquear CPF",
            description = "Rejeita uma solicitação pendente e impede novas solicitações para o CPF na mesma transação.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Solicitação rejeitada e CPF bloqueado."),
                    @ApiResponse(responseCode = "400", description = "Identificador ou motivo do bloqueio inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão MEMBERSHIP_MEMBERSHIP_APPLICANT_BLOCK.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Solicitação não encontrada.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Solicitação não está pendente ou o CPF já possui bloqueio ativo.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> rejectAndBlock(
            @Parameter(description = "Identificador da solicitação pendente.", example = "123")
            @PathVariable Long id,
            @Valid @RequestBody BlockMembershipApplicantRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        adminMembershipApplicationService.rejectAndBlock(id, principal.getUserId(), request.reason());
        return ResponseEntity.noContent().build();
    }
}
