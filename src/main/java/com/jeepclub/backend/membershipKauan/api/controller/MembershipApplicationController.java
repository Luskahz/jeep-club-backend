package com.jeepclub.backend.membershipKauan.api.controller;

import com.jeepclub.backend.membershipKauan.api.dto.membership.CreateMembershipApplicationRequestDTO;
import com.jeepclub.backend.membershipKauan.api.dto.membership.MembershipApplicationResponseDTO;
import com.jeepclub.backend.membershipKauan.core.application.service.CreateMembershipApplicationService;
import com.jeepclub.backend.membershipKauan.core.domain.model.MembershipApplication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/membership-applications")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Membership Applications",
        description = "Solicitações públicas de associação ao Jeep Club."
)
public class MembershipApplicationController {

    private final CreateMembershipApplicationService createMembershipApplicationService;

    @PostMapping
    @Operation(
            summary = "Criar solicitação de associação",
            description = "Registra uma nova solicitação pública de associação ao Jeep Club."
    )
    public ResponseEntity<MembershipApplicationResponseDTO> createMembershipApplication(
            @RequestBody @Valid CreateMembershipApplicationRequestDTO request
    ) {
        MembershipApplication application = createMembershipApplicationService.create(
                request.name(),
                request.cpf(),
                request.email(),
                request.phoneNumber(),
                request.message()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MembershipApplicationResponseDTO.from(application));
    }
}