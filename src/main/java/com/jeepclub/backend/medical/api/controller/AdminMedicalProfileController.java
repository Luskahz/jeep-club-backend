package com.jeepclub.backend.medical.api.controller;

import com.jeepclub.backend.medical.api.dto.MedicalProfileRequest;
import com.jeepclub.backend.medical.api.dto.MedicalProfileResponse;
import com.jeepclub.backend.medical.api.dto.MedicalProfileSummaryResponse;
import com.jeepclub.backend.medical.core.application.MedicalProfileService;
import com.jeepclub.backend.medical.core.domain.MedicalProfileOwnerType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/medical-profiles")
@RequiredArgsConstructor

@Tag(name = "Admin Medical Profiles", description = "Rotas administrativas de perfil médico.")
public class AdminMedicalProfileController {

    private final MedicalProfileService medicalProfileService;

    @GetMapping
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_READ')")
    @Operation(summary = "Lista perfis médicos de forma resumida para uso administrativo.")
    public ResponseEntity<List<MedicalProfileSummaryResponse>> listMedicalProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<MedicalProfileSummaryResponse> profiles = medicalProfileService
                .listMedicalProfiles(page, size)
                .stream()
                .map(MedicalProfileSummaryResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{profileId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_READ')")
    @Operation(summary = "Busca um perfil médico pelo ID do próprio perfil médico.")
    public ResponseEntity<MedicalProfileResponse> getByProfileId(
            @PathVariable Long profileId
    ) {
        var profile = medicalProfileService.getById(profileId);
        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_READ')")
    @Operation(summary = "Busca o perfil médico de um usuário específico.")
    public ResponseEntity<MedicalProfileResponse> getUserMedicalProfile(
            @PathVariable Long userId
    ) {
        var profile = medicalProfileService.getByOwner(
                MedicalProfileOwnerType.USER,
                userId
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_UPDATE')")
    @Operation(summary = "Cria ou atualiza o perfil médico de um usuário específico.")
    public ResponseEntity<MedicalProfileResponse> upsertUserMedicalProfile(
            @PathVariable Long userId,
            @Valid @RequestBody MedicalProfileRequest request
    ) {
        var profile = medicalProfileService.upsertByOwner(
                MedicalProfileOwnerType.USER,
                userId,
                request
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @GetMapping("/dependents/{dependentId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_READ')")
    @Operation(summary = "Busca o perfil médico de um dependente específico.")
    public ResponseEntity<MedicalProfileResponse> getDependentMedicalProfile(
            @PathVariable Long dependentId
    ) {
        var profile = medicalProfileService.getByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @PutMapping("/dependents/{dependentId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_UPDATE')")
    @Operation(summary = "Cria ou atualiza o perfil médico de um dependente específico.")
    public ResponseEntity<MedicalProfileResponse> upsertDependentMedicalProfile(
            @PathVariable Long dependentId,
            @Valid @RequestBody MedicalProfileRequest request
    ) {
        var profile = medicalProfileService.upsertByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId,
                request
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }
}
