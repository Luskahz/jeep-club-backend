package com.jeepclub.backend.iam.authentication.api.http.controller;

import com.jeepclub.backend.iam.authentication.api.http.dto.activation.ActivationInvitationStatusDTO;
import com.jeepclub.backend.iam.authentication.api.http.dto.activation.ActivationInvitationTokenDTO;
import com.jeepclub.backend.iam.authentication.api.http.dto.activation.CompleteAccountActivationDTO;
import com.jeepclub.backend.iam.authentication.core.application.service.activation.AccountActivationInvitationService;
import com.jeepclub.backend.platform.logging.SystemLogEvent;
import com.jeepclub.backend.platform.logging.SystemLogOutcome;
import com.jeepclub.backend.platform.logging.SystemLogService;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping(value = "/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication - Account Activation", description = "Convites administrativos para ativação de credenciais pendentes.")
public class AccountActivationController {
    private final AccountActivationInvitationService invitations;
    private final SystemLogService audit;
    private final Clock clock;

    @PostMapping(value = "/activation-invitations/validation", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(summary = "Validar convite sem consumi-lo", security = {}, responses = {
            @ApiResponse(responseCode = "204", description = "Convite válido."),
            @ApiResponse(responseCode = "400", description = "Token ausente ou malformado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "Convite inválido, expirado ou utilizado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))})
    public ResponseEntity<Void> validate(@RequestBody @Valid ActivationInvitationTokenDTO request) {
        invitations.validate(request.token());
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }

    @PostMapping(value = "/account-activations", consumes = MediaType.APPLICATION_JSON_VALUE)
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(summary = "Concluir ativação e definir senha permanente", description = "Consome o convite em transação e exige login posterior.", security = {}, responses = {
            @ApiResponse(responseCode = "204", description = "Conta ativada; efetue login."),
            @ApiResponse(responseCode = "400", description = "Token ou senha inválidos.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "Convite inválido, expirado ou utilizado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))})
    public ResponseEntity<Void> complete(@RequestBody @Valid CompleteAccountActivationDTO request) {
        invitations.complete(request.token(), request.newPassword());
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }

    @PostMapping(value = "/admin/users/{userId}/activation-invitations")
    @PreAuthorize("hasAuthority('IDENTITY_USER_CREATE')")
    @RequiredPermission("IDENTITY_USER_CREATE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(summary = "Emitir ou reenviar convite de ativação", description = "Exige conta pendente e e-mail. Rotaciona o token existente, sem recriar conta ou usuário.", responses = {
            @ApiResponse(responseCode = "202", description = "Envio preparado após commit.", content = @Content(schema = @Schema(implementation = ActivationInvitationStatusDTO.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem IDENTITY_USER_CREATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conta sem elegibilidade ou e-mail.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))})
    public ResponseEntity<ActivationInvitationStatusDTO> reissue(@PathVariable @Positive Long userId,
                                                                  @AuthenticationPrincipal UserPrincipal principal) {
        invitations.issueForExistingUser(userId);
        audit.record(new SystemLogEvent(principal.getUserId(), "ACCOUNT_ACTIVATION_INVITATION_REISSUE",
                "POST", "/authentication/admin/users/" + userId + "/activation-invitations",
                202, SystemLogOutcome.SUCCESS, 0, null, Instant.now(clock)));
        return ResponseEntity.accepted().header("Cache-Control", "no-store")
                .body(new ActivationInvitationStatusDTO(userId, "QUEUED"));
    }

    @GetMapping(value = "/admin/users/{userId}/activation-invitations")
    @PreAuthorize("hasAuthority('IDENTITY_USER_CREATE')")
    @RequiredPermission("IDENTITY_USER_CREATE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(summary = "Consultar status de entrega do convite", description = "Permite identificar DELIVERY_FAILED e reenviar. Nunca devolve token ou link.", responses = {
            @ApiResponse(responseCode = "200", description = "Status do convite.", content = @Content(schema = @Schema(implementation = ActivationInvitationStatusDTO.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem IDENTITY_USER_CREATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Convite ainda não emitido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))})
    public ResponseEntity<ActivationInvitationStatusDTO> status(@PathVariable @Positive Long userId) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(new ActivationInvitationStatusDTO(userId, invitations.deliveryStatus(userId)));
    }
}
