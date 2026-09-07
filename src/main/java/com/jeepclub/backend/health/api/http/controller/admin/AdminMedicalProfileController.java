package com.jeepclub.backend.health.api.http.controller.admin;

import com.jeepclub.backend.health.api.http.dto.MedicalProfileRequest;
import com.jeepclub.backend.health.api.http.dto.MedicalProfileMutationResponse;
import com.jeepclub.backend.health.api.http.dto.MedicalProfileResponse;
import com.jeepclub.backend.health.api.http.dto.MedicalProfileSummaryResponse;
import com.jeepclub.backend.health.core.application.service.medicalprofile.AdminMedicalProfileService;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Tag(
        name = "Health - Medical Profile",
        description = "Operações públicas e administrativas para consulta e gerenciamento do perfil médico."
)
public class AdminMedicalProfileController {

    private final AdminMedicalProfileService adminMedicalProfileService;

    @GetMapping
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_READ')")
    @Operation(summary = "Lista perfis médicos de forma resumida para uso administrativo.")
    public ResponseEntity<List<MedicalProfileSummaryResponse>> listMedicalProfiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<MedicalProfileSummaryResponse> profiles = adminMedicalProfileService
                .listMedicalProfiles(page, size, principal.getUserId())
                .stream()
                .map(MedicalProfileSummaryResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{profileId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_READ')")
    @Operation(summary = "Busca um perfil médico pelo ID do próprio perfil médico.")
    public ResponseEntity<MedicalProfileResponse> getByProfileId(
            @PathVariable Long profileId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var profile = adminMedicalProfileService.getById(
                profileId,
                principal.getUserId()
        );
        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_READ')")
    @Operation(summary = "Busca o perfil médico de um usuário específico.")
    public ResponseEntity<MedicalProfileResponse> getUserMedicalProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var profile = adminMedicalProfileService.getByOwner(
                MedicalProfileOwnerType.USER,
                userId,
                principal.getUserId()
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @PutMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_UPDATE')")
    @Operation(summary = "Cria ou atualiza o perfil médico de um usuário específico.")
    public ResponseEntity<MedicalProfileMutationResponse> upsertUserMedicalProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MedicalProfileRequest request
    ) {
        var profile = adminMedicalProfileService.upsertByOwner(
                MedicalProfileOwnerType.USER,
                userId,
                request.toApplicationData(),
                principal.getUserId()
        );

        return ResponseEntity.ok(MedicalProfileMutationResponse.fromDomain(profile));
    }

    @GetMapping("/dependents/{dependentId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_READ')")
    @Operation(summary = "Busca o perfil médico de um dependente específico.")
    public ResponseEntity<MedicalProfileResponse> getDependentMedicalProfile(
            @PathVariable Long dependentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var profile = adminMedicalProfileService.getByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId,
                principal.getUserId()
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @PutMapping("/dependents/{dependentId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_UPDATE')")
    @Operation(summary = "Cria ou atualiza o perfil médico de um dependente específico.")
    public ResponseEntity<MedicalProfileMutationResponse> upsertDependentMedicalProfile(
            @PathVariable Long dependentId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MedicalProfileRequest request
    ) {
        var profile = adminMedicalProfileService.upsertByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependentId,
                request.toApplicationData(),
                principal.getUserId()
        );

        return ResponseEntity.ok(MedicalProfileMutationResponse.fromDomain(profile));
    }

    @DeleteMapping("/{profileId}")
    @PreAuthorize("hasAuthority('HEALTH_MEDICAL_PROFILE_DELETE')")
    @Operation(
            summary = "Exclui um perfil médico pelo ID.",
            description = "Arquiva o histórico e remove o perfil médico operacional."
    )
    public ResponseEntity<Void> deleteByProfileId(
            @PathVariable Long profileId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        adminMedicalProfileService.deleteById(
                profileId,
                principal.getUserId()
        );

        return ResponseEntity.noContent().build();
    }
}
