package com.jeepclub.backend.memberships.api.http.controller;

import com.jeepclub.backend.memberships.api.http.dto.AccessLinkApprovalResponseDTO;
import com.jeepclub.backend.memberships.api.http.dto.MembershipApplicationResponseDTO;
import com.jeepclub.backend.memberships.api.http.dto.RejectMembershipRequestDTO;
import com.jeepclub.backend.memberships.api.http.dto.TemporaryPasswordApprovalResponseDTO;
import com.jeepclub.backend.memberships.core.application.service.membershipactivationtoken.AdminMembershipActivationTokenService;
import com.jeepclub.backend.memberships.core.application.service.membershipapplication.AdminMembershipService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/membership-applications")
@RequiredArgsConstructor
@Tag(name = "Membership Admin", description = "Gerenciamento de solicitações de adesão pelo admin.")
public class AdminMembershipApplicationController {

    private final AdminMembershipService adminMembershipService;
    private final AdminMembershipActivationTokenService adminMembershipActivationTokenService;

    @GetMapping
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_READ')")
    @Operation(summary = "Listar solicitações", description = "Lista todas ou filtra por status. Suporta paginação.")
    public ResponseEntity<Page<MembershipApplicationResponseDTO>> list(
            @RequestParam(required = false) MembershipApplicationStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Page<MembershipApplicationResponseDTO> response = (status != null
                ? adminMembershipService.listByStatus(status, pageable)
                : adminMembershipService.listAll(pageable))
                .map(MembershipApplicationResponseDTO::fromDomain);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_READ')")
    @Operation(summary = "Buscar solicitação por ID", description = "Retorna todos os campos da solicitação.")
    public ResponseEntity<MembershipApplicationResponseDTO> getById(@PathVariable Long id) {
        return adminMembershipService.findById(id)
                .map(MembershipApplicationResponseDTO::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/approve/temporary-password")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE')")
    @Operation(
            summary = "Aprovar solicitação com senha temporária",
            description = "Cria o usuário com PENDING_FIRST_ACCESS e retorna a senha temporária uma única vez para o administrador."
    )
    public ResponseEntity<TemporaryPasswordApprovalResponseDTO> approveWithTemporaryPassword(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var result = adminMembershipService.approveWithTemporaryPassword(
                id,
                principal.getUserId()
        );

        return ResponseEntity.ok(TemporaryPasswordApprovalResponseDTO.from(result));
    }

    @PostMapping("/{id}/approve/access-link")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE')")
    @Operation(
            summary = "Aprovar solicitação com link de acesso",
            description = "Cria o usuário com PENDING_FIRST_ACCESS e retorna um link com token para definição da senha."
    )
    public ResponseEntity<AccessLinkApprovalResponseDTO> approveWithAccessLink(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var result = adminMembershipActivationTokenService.approveWithAccessLink(
                id,
                principal.getUserId()
        );

        return ResponseEntity.ok(AccessLinkApprovalResponseDTO.from(result));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT')")
    @Operation(summary = "Rejeitar solicitação", description = "Motivo é opcional.")
    public ResponseEntity<Void> reject(
            @PathVariable Long id,
            @RequestBody(required = false) RejectMembershipRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        String reason = request != null ? request.reason() : null;
        adminMembershipService.reject(id, principal.getUserId(), reason);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
