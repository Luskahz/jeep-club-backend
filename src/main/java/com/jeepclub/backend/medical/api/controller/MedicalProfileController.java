package com.jeepclub.backend.medical.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import com.jeepclub.backend.medical.api.dto.MedicalProfileRequest;
import com.jeepclub.backend.medical.api.dto.MedicalProfileResponse;
import com.jeepclub.backend.medical.core.application.MedicalProfileService;
import com.jeepclub.backend.medical.core.domain.MedicalProfileOwnerType;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/medical-profiles")
@RequiredArgsConstructor

// importante, separe as rotas administrativas das rotas publicas, se quiser pode ir criando um controller: AdminMedicalProfile para as futuras rotas administrativas

public class MedicalProfileController {

    private final MedicalProfileService medicalProfileService;

    @GetMapping("/me")
    public ResponseEntity<MedicalProfileResponse> getMyMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal.getUserId();

        var profile = medicalProfileService.getByOwner(
                MedicalProfileOwnerType.USER,
                userId
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }


    // acho que não temos estrutura suficiente pra ser necessario uma rota put no resumo me do seu modulo, é valido, porem caso o frontend
    // queira realizar atualizações nos dados do resumo(a rota me é um resumo util pro frontend do modulo relacionado ao user)
    // o frontend pode simplismente atualizar nas rotas especifica que não é a me, fica menos semantico.
    @PutMapping("/me")
    public ResponseEntity<MedicalProfileResponse> upsertMyMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MedicalProfileRequest request
    ) {
        Long userId = principal.getUserId();

        var profile = medicalProfileService.upsert(
                MedicalProfileOwnerType.USER,
                userId,
                request
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }



    // ambas as rotas abaixo estão ok, porem ainda precisam ser desenroladas
    // entendi sua questão sobre ter que definir se oque está sendo buscado são os dados medicinais do user ou do dependente
    // e sim, vc precisa dar o id do dependente no path, e no service vc vai validar se o dependente pertence a este usuario.
    // pra isso vc vai usar o metodo de port e adapter vai falar com o daniel para vc criar um port no seu infra com uma função de "verificaçãoSeODependenteÉDesteUser"
    // e o daniel vai implementar no infra dele um adapter que implementa sua porta, ai pra implementar sua porta ele vai usar os repository que ele tem no dependents
    @GetMapping("/dependents/{dependentId}")
    public ResponseEntity<MedicalProfileResponse> getDependentMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long dependentId
    ) {
        /*
         * IMPORTANTE:
         * Antes de buscar, você deve validar se esse dependente pertence ao usuário logado.
         * Essa validação deve entrar quando o módulo de dependentes estiver pronto.
         */

        var profile = medicalProfileService.getByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @PutMapping("/dependents/{dependentId}")
    public ResponseEntity<MedicalProfileResponse> upsertDependentMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long dependentId,
            @Valid @RequestBody MedicalProfileRequest request
    ) {
        /*
         * IMPORTANTE:
         * Antes de salvar, você deve validar se esse dependente pertence ao usuário logado.
         */

        var profile = medicalProfileService.upsert(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId,
                request
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }
}