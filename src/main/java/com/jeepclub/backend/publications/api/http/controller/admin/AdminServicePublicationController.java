package com.jeepclub.backend.publications.api.http.controller.admin;

import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.publications.api.http.dto.ServicePublicationResponseDTO;
import com.jeepclub.backend.publications.core.application.service.ServicePublicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/services")
@RequiredArgsConstructor
@Tag(name = "Publications - Service Admin", description = "Consulta e exclusão administrativa de serviços.")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permission ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Service não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AdminServicePublicationController {
    private final ServicePublicationService services;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_ADMIN_READ')")
    @RequiredPermission("PUBLICATIONS_SERVICE_ADMIN_READ")
    @Operation(summary = "Consultar serviço administrativamente", description = "Pode consultar Service em qualquer estado editorial.")
    @ApiResponse(responseCode = "200", description = "Service encontrado.", content = @Content(schema = @Schema(implementation = ServicePublicationResponseDTO.class)))
    public ServicePublicationResponseDTO find(@PathVariable Long id) {
        return ServicePublicationResponseDTO.from(services.findAdmin(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_ADMIN_DELETE')")
    @RequiredPermission("PUBLICATIONS_SERVICE_ADMIN_DELETE")
    @Operation(summary = "Excluir qualquer serviço", description = "Hard delete com snapshot histórico; requests e objetos globais de mídia permanecem.")
    @ApiResponse(responseCode = "204", description = "Service excluído com histórico.")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        services.deleteAdmin(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
