package com.jeepclub.backend.memberships.api.http.controller;

import com.jeepclub.backend.memberships.api.http.dto.CreateMembershipApplicationRequestDTO;
import com.jeepclub.backend.memberships.api.http.dto.MembershipApplicationResponseDTO;
import com.jeepclub.backend.memberships.core.application.result.EnsureMembershipRequestResult;
import com.jeepclub.backend.memberships.core.application.service.EnsureMembershipRequestService;
import io.swagger.v3.oas.annotations.Operation;
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

    private final EnsureMembershipRequestService ensureService;

    @PostMapping
    @Operation(
            summary = "Solicitar adesão ao clube",
            description = """
                Rota pública. Se já existir uma solicitação aberta para o CPF informado,
                retorna a solicitação existente. Caso contrário, cria uma nova.
                """
    )
    public ResponseEntity<MembershipApplicationResponseDTO> create(
            @Valid @RequestBody CreateMembershipApplicationRequestDTO request
    ) {
        EnsureMembershipRequestResult result = ensureService.ensure(
                request.name(),
                request.cpf(),
                request.email(),
                request.phoneNumber(),
                request.message()
        );

        MembershipApplicationResponseDTO response =
                MembershipApplicationResponseDTO.fromDomain(
                        result.application()
                );

        return ResponseEntity
                .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(response);
    }
}