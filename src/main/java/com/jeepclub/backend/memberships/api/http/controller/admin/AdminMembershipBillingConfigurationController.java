package com.jeepclub.backend.memberships.api.http.controller.admin;

import com.jeepclub.backend.memberships.api.http.dto.MembershipBillingConfigurationRequest;
import com.jeepclub.backend.memberships.api.http.dto.MembershipBillingConfigurationResponse;
import com.jeepclub.backend.memberships.api.http.dto.MembershipBillingEnforcementRequest;
import com.jeepclub.backend.memberships.core.application.service.membershipbilling.AdminMembershipBillingConfigurationService;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/membership-billing-configuration")
@RequiredArgsConstructor
@Tag(name = "Membership Billing Admin", description = "Configuração da cobrança que representa a membritude.")
public class AdminMembershipBillingConfigurationController {

    private final AdminMembershipBillingConfigurationService service;

    @GetMapping
    @PreAuthorize("hasAuthority('MEMBERSHIP_BILLING_CONFIGURATION_READ')")
    @RequiredPermission("MEMBERSHIP_BILLING_CONFIGURATION_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(summary = "Consultar configuração financeira da membritude", responses = {
            @ApiResponse(responseCode = "200", description = "Configuração atual.", content = @Content(schema = @Schema(implementation = MembershipBillingConfigurationResponse.class))),
            @ApiResponse(responseCode = "204", description = "A membritude financeira ainda não foi configurada."),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Permission ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<MembershipBillingConfigurationResponse> getCurrent() {
        return service.getCurrent()
                .map(MembershipBillingConfigurationResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('MEMBERSHIP_BILLING_CONFIGURATION_UPDATE')")
    @RequiredPermission("MEMBERSHIP_BILLING_CONFIGURATION_UPDATE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(summary = "Configurar ou substituir a cobrança de membritude", responses = {
            @ApiResponse(responseCode = "200", description = "Configuração salva.", content = @Content(schema = @Schema(implementation = MembershipBillingConfigurationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Permission ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "ChargeDefinition inexistente ou não ativa.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<MembershipBillingConfigurationResponse> configure(
            @Valid @RequestBody MembershipBillingConfigurationRequest request
    ) {
        return ResponseEntity.ok(MembershipBillingConfigurationResponse.from(
                service.configure(request.chargeDefinitionId(), request.enforcementEnabled())
        ));
    }

    @PatchMapping("/enforcement")
    @PreAuthorize("hasAuthority('MEMBERSHIP_BILLING_CONFIGURATION_UPDATE')")
    @RequiredPermission("MEMBERSHIP_BILLING_CONFIGURATION_UPDATE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(summary = "Habilitar ou desabilitar a exigência financeira", description = "Preserva a ChargeDefinition configurada.", responses = {
            @ApiResponse(responseCode = "200", description = "Estado atualizado.", content = @Content(schema = @Schema(implementation = MembershipBillingConfigurationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Permission ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Configuração ainda não existente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<MembershipBillingConfigurationResponse> setEnforcement(
            @Valid @RequestBody MembershipBillingEnforcementRequest request
    ) {
        return ResponseEntity.ok(MembershipBillingConfigurationResponse.from(
                service.setEnforcementEnabled(request.enforcementEnabled())
        ));
    }
}
