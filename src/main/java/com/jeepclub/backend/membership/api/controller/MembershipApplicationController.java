package com.jeepclub.backend.membership.api.controller;

import com.jeepclub.backend.membership.api.dto.CreateMembershipApplicationRequestDTO;
import com.jeepclub.backend.membership.api.dto.MembershipApplicationResponseDTO;
import com.jeepclub.backend.membership.core.application.service.CreateMembershipApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/membership/request")
@RequiredArgsConstructor
@Tag(name = "Membership", description = "Solicitação pública de adesão ao clube.")
public class MembershipApplicationController {

    private final CreateMembershipApplicationService createService;

    @PostMapping
    @Operation(
            summary = "Solicitar adesão ao clube",
            description = "Rota pública. Candidato envia seus dados para análise do admin."
    )
    public ResponseEntity<MembershipApplicationResponseDTO> create(
            @RequestBody @Valid CreateMembershipApplicationRequestDTO request
    ) {
        MembershipApplicationResponseDTO response = createService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}