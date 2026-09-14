package com.jeepclub.backend.health.api.http.controller;

import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.health.api.http.dto.MedicalProfileRequest;
import com.jeepclub.backend.health.api.http.dto.MedicalProfileResponse;
import com.jeepclub.backend.health.core.application.service.medicalprofile.MedicalProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/medical-profiles")
@RequiredArgsConstructor
@Tag(
        name = "Health - Medical Profile",
        description = "Operações do usuário autenticado sobre o próprio perfil médico e os perfis de seus dependentes ativos."
)
public class MedicalProfileController {

    private final MedicalProfileService medicalProfileService;

    @GetMapping("/me")
    @Operation(
            summary = "Consultar o próprio perfil médico",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Perfil médico retornado.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MedicalProfileResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Perfil médico ou usuário proprietário não encontrado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "O usuário proprietário está inativo.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Falha não transitória ao acessar a persistência do perfil.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Validação de owner ou persistência temporariamente indisponível.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MedicalProfileResponse> getMyMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var profile = medicalProfileService.getMyMedicalProfile(
                principal.getUserId()
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @PutMapping("/me")
    @Operation(
            summary = "Criar ou atualizar o próprio perfil médico",
            description = "Faz upsert: cria o perfil se ele não existir ou atualiza o perfil operacional já existente.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Perfil criado ou atualizado.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MedicalProfileResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Dados médicos inválidos.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Usuário proprietário não encontrado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "O owner está inativo ou houve conflito concorrente no perfil.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Falha não transitória ao persistir o perfil.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Persistência temporariamente indisponível.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MedicalProfileResponse> upsertMyMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MedicalProfileRequest request
    ) {
        var profile = medicalProfileService.upsertMyMedicalProfile(
                principal.getUserId(),
                request.toApplicationData()
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @GetMapping("/dependents/{dependentId}")
    @Operation(
            summary = "Consultar o perfil médico de um dependente",
            description = "Exige que o usuário autenticado e o dependente estejam ativos e que o dependente pertença ao usuário.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Perfil médico retornado.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MedicalProfileResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Formato do identificador do dependente inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "O dependente não pertence ao usuário autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Perfil, usuário ou dependente não encontrado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "O usuário ou dependente está inativo.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Falha não transitória ao acessar a persistência do perfil.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Validação de vínculo ou persistência temporariamente indisponível.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MedicalProfileResponse> getDependentMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Identificador do dependente proprietário do perfil.", example = "10", required = true)
            @PathVariable Long dependentId
    ) {
        var profile = medicalProfileService.getDependentMedicalProfile(
                principal.getUserId(),
                dependentId
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @PutMapping("/dependents/{dependentId}")
    @Operation(
            summary = "Criar ou atualizar o perfil médico de um dependente",
            description = "Exige o vínculo ativo com o dependente e faz upsert do perfil desse dependente.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Perfil criado ou atualizado.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MedicalProfileResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Identificador ou dados médicos inválidos.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "O dependente não pertence ao usuário autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Usuário ou dependente não encontrado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "O usuário ou dependente está inativo, ou houve conflito concorrente no perfil.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Falha não transitória ao persistir o perfil.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Validação de vínculo ou persistência temporariamente indisponível.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MedicalProfileResponse> upsertDependentMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Identificador do dependente proprietário do perfil.", example = "10", required = true)
            @PathVariable Long dependentId,
            @Valid @RequestBody MedicalProfileRequest request
    ) {
        var profile = medicalProfileService.upsertDependentMedicalProfile(
                principal.getUserId(),
                dependentId,
                request.toApplicationData()
        );

        return ResponseEntity.ok(MedicalProfileResponse.fromDomain(profile));
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "Exclui o perfil médico do usuário autenticado.",
            description = "Cria um snapshot histórico e remove o perfil médico operacional.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Perfil excluído e histórico preservado."),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Perfil médico ou usuário proprietário não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "O owner está inativo, o perfil já foi removido ou houve conflito concorrente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Falha não transitória ao remover o perfil.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Persistência temporariamente indisponível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> deleteMyMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        medicalProfileService.deleteMyMedicalProfile(
                principal.getUserId()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/dependents/{dependentId}")
    @Operation(
            summary = "Exclui o perfil médico de um dependente.",
            description = "Valida o vínculo ativo, cria um snapshot histórico e remove o perfil operacional.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Perfil excluído e histórico preservado."),
                    @ApiResponse(responseCode = "400", description = "Formato do identificador do dependente inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "O dependente não pertence ao usuário autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Perfil, usuário ou dependente não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "O owner está inativo, o perfil já foi removido ou houve conflito concorrente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Falha não transitória ao remover o perfil.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "Validação de vínculo ou persistência temporariamente indisponível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> deleteDependentMedicalProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Identificador do dependente proprietário do perfil.", example = "10", required = true)
            @PathVariable Long dependentId
    ) {
        medicalProfileService.deleteDependentMedicalProfile(
                principal.getUserId(),
                dependentId
        );

        return ResponseEntity.noContent().build();
    }
}
