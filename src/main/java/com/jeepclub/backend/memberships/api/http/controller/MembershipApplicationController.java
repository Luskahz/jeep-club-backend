package com.jeepclub.backend.memberships.api.http.controller;

import com.jeepclub.backend.memberships.api.http.dto.CreateMembershipApplicationRequestDTO;
import com.jeepclub.backend.memberships.api.http.dto.MembershipApplicationResponseDTO;
import com.jeepclub.backend.memberships.core.application.result.EnsureMembershipRequestResult;
import com.jeepclub.backend.memberships.core.application.service.membershipactivationtoken.MembershipActivationTokenService;
import com.jeepclub.backend.memberships.core.application.service.membershipapplication.MembershipApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/membership-applications")
@RequiredArgsConstructor
@Tag(name = "Membership", description = "Solicitação pública de adesão ao clube.")
public class MembershipApplicationController {

    private final MembershipApplicationService membershipApplicationService;
    private final MembershipActivationTokenService membershipActivationTokenService;

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
        EnsureMembershipRequestResult result = membershipApplicationService.ensure(
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

    @GetMapping("/activate")
    @Operation(
            summary = "Validar token de ativação",
            description = """
                    Rota pública. Chamada quando o candidato clica no link recebido por e-mail.
                    Valida o token, marca como utilizado e retorna o ID da solicitação confirmada.
                    A partir deste ponto o frontend pode redirecionar para o fluxo de criação de senha.
                    """
    )
    public ResponseEntity<Map<String, Object>> activate(
            @Parameter(description = "Token de ativação recebido por e-mail.", required = true)
            @RequestParam String token
    ) {
        Long applicationId = membershipActivationTokenService.validate(token);

        return ResponseEntity.ok(Map.of(
                "message", "Token validado com sucesso.",
                "applicationId", applicationId
        ));
    }
}
