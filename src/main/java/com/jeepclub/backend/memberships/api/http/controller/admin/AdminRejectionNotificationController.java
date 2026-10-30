package com.jeepclub.backend.memberships.api.http.controller.admin;

import com.jeepclub.backend.memberships.core.application.service.notification.RejectionNotificationService;
import com.jeepclub.backend.memberships.core.repository.RejectionNotificationRepository.Status;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/membership-applications/{id}/rejection-notification")
@RequiredArgsConstructor
@Tag(name = "Membership Admin")
public class AdminRejectionNotificationController {
    private final RejectionNotificationService service;
    public record AttemptResponse(String result) { }

    @GetMapping
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_READ')")
    @RequiredPermission("MEMBERSHIP_MEMBERSHIP_REQUEST_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(summary = "Consultar notificação de rejeição", description = "Metadados e histórico sanitizado, sem destinatário ou motivo.", responses = {
        @ApiResponse(responseCode = "200", description = "Estado e histórico da notificação."),
        @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Sem permissão de leitura.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Solicitação ou notificação inexistente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Status> status(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.status(id));
    }

    @PostMapping("/resend")
    @PreAuthorize("hasAuthority('MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT')")
    @RequiredPermission("MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(summary = "Reenviar notificação de rejeição", description = "Tentativa MANUAL imediata e exclusiva, inclusive após esgotar as três automáticas. Não reinicia o ciclo. SMTP_ACCEPTED significa apenas aceite pelo provedor.", responses = {
        @ApiResponse(responseCode = "200", description = "SMTP_ACCEPTED: provedor aceitou, sem confirmação de entrega/leitura. SIMULATED: execução simulada em dev/test, sem aceite SMTP."),
        @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Sem permissão de rejeição administrativa.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Solicitação inexistente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Não rejeitada, sem destinatário ou notificação em processamento.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "502", description = "Falha operacional SMTP registrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "503", description = "Resultado não confirmado no banco. Pode haver aceite SMTP; consultar histórico antes de repetir.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<AttemptResponse> resend(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new AttemptResponse(service.resend(id, principal.getUserId()).name()));
    }
}
