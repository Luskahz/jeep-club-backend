package com.jeepclub.backend.publications.api.http.controller.member;

import com.jeepclub.backend.memberships.api.security.RequiresMembership;
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
@RequestMapping("/services")
@RequiresMembership
@RequiredArgsConstructor
@Tag(name = "Publications - Services", description = "Leitura de serviços publicados e ações do proprietário.")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "402", description = "Membership ativa exigida.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permission ou ownership ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Service ou mídia não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Estado editorial ou change request pendente conflitante.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class ServicePublicationController {
    private final ServicePublicationService services;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_READ')")
    @RequiredPermission("PUBLICATIONS_SERVICE_READ")
    @Operation(summary = "Consultar serviço publicado", description = "Retorna somente a versão pública PUBLISHED; uma proposta pendente nunca altera esta leitura.")
    @ApiResponse(responseCode = "200", description = "Service publicado.", content = @Content(schema = @Schema(implementation = ServicePublicationResponseDTO.class)))
    public ServicePublicationResponseDTO findPublished(@PathVariable Long id) {
        return ServicePublicationResponseDTO.from(services.findPublished(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_DELETE')")
    @RequiredPermission("PUBLICATIONS_SERVICE_DELETE")
    @Operation(summary = "Excluir serviço próprio", description = "Ownership e permission são exigidos; hard delete salva snapshot histórico. Requests e objetos globais de mídia permanecem.")
    @ApiResponse(responseCode = "204", description = "Service excluído com histórico.")
    public ResponseEntity<Void> deleteOwn(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        services.deleteOwn(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
