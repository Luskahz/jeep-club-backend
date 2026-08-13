package com.jeepclub.backend.memberships.api.http.controller;

import com.jeepclub.backend.memberships.core.application.service.membershipapplicantblock.MembershipApplicantBlockService;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    private final MembershipApplicantBlockService membershipApplicantBlockService;

    @PostMapping("/{cpf}/unblock")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_APPLICANT_UNBLOCK')")
    @Operation(
            summary = "Desbloquear CPF",
            description = "Encerra o bloqueio ativo do CPF preservando o histórico."
    )
    public ResponseEntity<Void> unblock(
            @PathVariable String cpf,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        membershipApplicantBlockService.unblock(cpf, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
