package com.jeepclub.backend.memberships.api.http.controller;

import com.jeepclub.backend.memberships.core.application.service.memberactivationtoken.MemberActivationTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/membership-applications")
@RequiredArgsConstructor
@Tag(name = "Membership", description = "Solicitação pública de adesão ao clube.")
public class MemberActivationTokenController {

    private final MemberActivationTokenService memberActivationTokenService;

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
        Long applicationId = memberActivationTokenService.validate(token);

        return ResponseEntity.ok(Map.of(
                "message", "Token validado com sucesso.",
                "applicationId", applicationId
        ));
    }
}
