package com.jeepclub.backend.medical.api.controller;

import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import com.jeepclub.backend.medical.api.dto.MedicalProfileRequest;
import com.jeepclub.backend.medical.api.dto.MedicalProfileResponse;
import com.jeepclub.backend.medical.core.application.MedicalProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/medical-profiles")
@RequiredArgsConstructor
@Tag(name = "Medical Profiles", description = "Rotas de perfil médico do usuário autenticado e seus dependentes.")
public class MedicalProfileController {

    private final MedicalProfileService medicalProfileService;

    @GetMapping("/me")
    @Operation(summary = "Busca o perfil médico do usuário autenticado.")
    public ResponseEntity<MedicalProfileResponse> getMyMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var profile = medicalProfileService.getMyMedicalProfile(
                principal.getUserId()
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @PutMapping("/me")
    @Operation(summary = "Cria ou atualiza o perfil médico do usuário autenticado.")
    public ResponseEntity<MedicalProfileResponse> upsertMyMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MedicalProfileRequest request
    ) {
        var profile = medicalProfileService.upsertMyMedicalProfile(
                principal.getUserId(),
                request
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @GetMapping("/dependents/{dependentId}")
    @Operation(summary = "Busca o perfil médico de um dependente do usuário autenticado.")
    public ResponseEntity<MedicalProfileResponse> getDependentMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long dependentId
    ) {
        var profile = medicalProfileService.getDependentMedicalProfile(
                principal.getUserId(),
                dependentId
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @PutMapping("/dependents/{dependentId}")
    @Operation(summary = "Cria ou atualiza o perfil médico de um dependente do usuário autenticado.")
    public ResponseEntity<MedicalProfileResponse> upsertDependentMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long dependentId,
            @Valid @RequestBody MedicalProfileRequest request
    ) {
        var profile = medicalProfileService.upsertDependentMedicalProfile(
                principal.getUserId(),
                dependentId,
                request
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }
}
