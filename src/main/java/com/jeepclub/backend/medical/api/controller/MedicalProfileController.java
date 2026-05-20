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