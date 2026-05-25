package com.jeepclub.backend.membership.api.controller;

import com.jeepclub.backend.membership.api.dto.MembershipApplicationResponseDTO;
import com.jeepclub.backend.membership.api.dto.RejectMembershipRequestDTO;
import com.jeepclub.backend.membership.core.application.service.ApproveMembershipApplicationService;
import com.jeepclub.backend.membership.core.application.service.ListMembershipApplicationsService;
import com.jeepclub.backend.membership.core.application.service.RejectMembershipApplicationService;
import com.jeepclub.backend.membership.core.application.service.ResendActivationTokenService;
import com.jeepclub.backend.membership.core.domain.enums.MembershipApplicationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PreAuthorize("hasAuthority('MEMBERSHIP_APPLICATION_READ')")
    @Operation(summary = "Listar solicitações", description = "Lista todas ou filtra por status.")
    public ResponseEntity<List<MembershipApplicationResponseDTO>> list(
            @RequestParam(required = false) MembershipApplicationStatus status
    ) {
        List<MembershipApplicationResponseDTO> response = (status != null
                ? listService.listByStatus(status)
                : listService.listAll())
                .stream()
                .map(MembershipApplicationResponseDTO::fromDomain)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MEMBERSHIP_APPLICATION_READ')")
    @Operation(summary = "Buscar solicitação por ID")
    public ResponseEntity<MembershipApplicationResponseDTO> getById(@PathVariable Long id) {
        return listService.findById(id)
                .map(MembershipApplicationResponseDTO::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MEMBERSHIP_APPLICATION_APPROVE')")
    @Operation(summary = "Aprovar solicitação", description = "Cria usuário com PENDING_FIRST_ACCESS e envia link por e-mail.")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        approveService.approve(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MEMBERSHIP_APPLICATION_REJECT')")
    @Operation(summary = "Rejeitar solicitação", description = "Motivo é opcional.")
    public ResponseEntity<Void> reject(
            @PathVariable Long id,
            @RequestBody(required = false) RejectMembershipRequestDTO request
    ) {
        String reason = request != null ? request.reason() : null;
        rejectService.reject(id, reason);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/resend-invite")
    @PreAuthorize("hasAuthority('MEMBERSHIP_APPLICATION_APPROVE')")
    @Operation(summary = "Reenviar convite de ativação")
    public ResponseEntity<Void> resendInvite(@PathVariable Long id) {
        resendService.resend(id);
        return ResponseEntity.noContent().build();
    }
}