package com.jeepclub.backend.memberships.api.http.controller;

import com.jeepclub.backend.memberships.api.http.dto.MembershipApplicationResponseDTO;
import com.jeepclub.backend.memberships.api.http.dto.RejectMembershipRequestDTO;
import com.jeepclub.backend.memberships.core.application.service.ApproveMembershipApplicationService;
import com.jeepclub.backend.memberships.core.application.service.ListMembershipApplicationsService;
import com.jeepclub.backend.memberships.core.application.service.RejectMembershipApplicationService;
import com.jeepclub.backend.memberships.core.application.service.ResendActivationTokenService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/membership/admin")
@RequiredArgsConstructor
@Tag(name = "Membership Admin", description = "Gerenciamento de solicitações de adesão pelo admin.")
public class MembershipManagerController {

    private final ApproveMembershipApplicationService approveService;
    private final RejectMembershipApplicationService rejectService;
    private final ResendActivationTokenService resendService;
    private final ListMembershipApplicationsService listService;

    @GetMapping
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_READ')")
    @Operation(summary = "Listar solicitações", description = "Lista todas ou filtra por status. Suporta paginação.")
    public ResponseEntity<Page<MembershipApplicationResponseDTO>> list(
            @RequestParam(required = false) MembershipApplicationStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Page<MembershipApplicationResponseDTO> response = (status != null
                ? listService.listByStatus(status, pageable)
                : listService.listAll(pageable))
                .map(MembershipApplicationResponseDTO::fromDomain);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_READ')")
    @Operation(summary = "Buscar solicitação por ID", description = "Retorna todos os campos da solicitação.")
    public ResponseEntity<MembershipApplicationResponseDTO> getById(@PathVariable Long id) {
        return listService.findById(id)
                .map(MembershipApplicationResponseDTO::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE')")
    @Operation(summary = "Aprovar solicitação", description = "Cria usuário com PENDING_FIRST_ACCESS e envia link por e-mail.")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        approveService.approve(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT')")
    @Operation(summary = "Rejeitar solicitação", description = "Motivo é opcional.")
    public ResponseEntity<Void> reject(
            @PathVariable Long id,
            @RequestBody(required = false) RejectMembershipRequestDTO request
    ) {
        String reason = request != null ? request.reason() : null;
        rejectService.reject(id, reason);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{id}/resend-invite")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_INVITE_RESEND')")
    @Operation(summary = "Reenviar convite de ativação", description = "Invalida o token anterior e envia um novo link de ativação por e-mail.")
    public ResponseEntity<Void> resendInvite(@PathVariable Long id) {
        resendService.resend(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}