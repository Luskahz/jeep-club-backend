package com.jeepclub.backend.membership.api.controller;

import com.jeepclub.backend.membership.api.dto.MembershipApplicationResponseDTO;
import com.jeepclub.backend.authentication.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.authentication.core.domain.model.MembershipApplication;
import com.jeepclub.backend.membership.api.dto.RejectMembershipRequestDTO;
import com.jeepclub.backend.membership.core.application.service.ApproveMembershipApplicationService;
import com.jeepclub.backend.membership.core.application.service.ListMembershipApplicationsService;
import com.jeepclub.backend.membership.core.application.service.RejectMembershipApplicationService;
import com.jeepclub.backend.membership.core.application.service.ResendActivationTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/membership-manager/applications")
@RequiredArgsConstructor
public class MembershipManagerController {

    private final ApproveMembershipApplicationService approveService;
    private final RejectMembershipApplicationService rejectService;
    private final ResendActivationTokenService resendService;
    private final ListMembershipApplicationsService listService;

    @GetMapping
    @PreAuthorize("hasAuthority('MEMBERSHIP_MANAGER_APPLICATION_READ')")
    public ResponseEntity<List<MembershipApplicationResponseDTO>> list(
            @RequestParam(required = false) MembershipApplicationStatus status
    ) {
        List<MembershipApplication> applications = status != null
                ? listService.listByStatus(status)
                : listService.listAll();

        List<MembershipApplicationResponseDTO> response = applications.stream()
                .map(MembershipApplicationResponseDTO::fromDomain)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MANAGER_APPLICATION_APPROVE')")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        approveService.approve(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MANAGER_APPLICATION_REJECT')")
    public ResponseEntity<Void> reject(
            @PathVariable Long id,
            @RequestBody(required = false) RejectMembershipRequestDTO request
    ) {
        String reason = request != null ? request.reason() : null;
        rejectService.reject(id, reason);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/resend-invite")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MANAGER_APPLICATION_RESEND_INVITE')")
    public ResponseEntity<Void> resendInvite(@PathVariable Long id) {
        resendService.resend(id);
        return ResponseEntity.noContent().build();
    }
}