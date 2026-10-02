package com.jeepclub.backend.publications.api.http.controller.member;

import com.jeepclub.backend.memberships.api.security.RequiresMembership;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.publications.api.http.dto.PublicationImageRequestDTO;
import com.jeepclub.backend.publications.api.http.dto.ServicePublicationChangeRequestResponseDTO;
import com.jeepclub.backend.publications.api.http.dto.ServiceRequestReader;
import com.jeepclub.backend.publications.api.http.dto.UpdateServicePublicationRequestDTO;
import com.jeepclub.backend.publications.core.application.service.ServicePublicationChangeRequestService;
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
import tools.jackson.databind.JsonNode;

@RestController
@RequiresMembership
@RequiredArgsConstructor
@Tag(name = "Publications - Service Changes", description = "Consulta de propostas de alteração próprias.")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "402", description = "Membership ativa exigida.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permission ou ownership ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Change request, Service ou mídia não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class ServicePublicationChangeRequestController {
    private final ServicePublicationChangeRequestService changes;
    private final ServiceRequestReader reader;

    @PatchMapping(value = "/services/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE')")
    @RequiredPermission("PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE")
    @Operation(summary = "Solicitar alteração do próprio serviço", description = "Cria uma change request PENDING e não altera o Service. Omitido preserva, null explícito e body vazio são inválidos; imagens enviadas substituem a galeria completa.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = UpdateServicePublicationRequestDTO.class))))
    @ApiResponse(responseCode = "201", description = "Change request criada.", content = @Content(schema = @Schema(implementation = ServicePublicationChangeRequestResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Service não editável ou proposta pendente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ServicePublicationChangeRequestResponseDTO> requestChange(@PathVariable Long id,
            @RequestBody JsonNode body, @AuthenticationPrincipal UserPrincipal principal) {
        var request = reader.update(body);
        var images = request.images() == null ? null : request.images().stream().map(PublicationImageRequestDTO::toDomain).toList();
        return ResponseEntity.status(201).body(ServicePublicationChangeRequestResponseDTO.from(changes.create(
                id, principal.getUserId(), request.title(), request.content(), request.amount(), request.contactPhone(), images)));
    }

    @GetMapping("/service-publication-change-requests/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_CHANGE_REQUEST_READ')")
    @RequiredPermission("PUBLICATIONS_SERVICE_CHANGE_REQUEST_READ")
    @Operation(summary = "Consultar alteração própria", description = "A proposta de outro usuário é tratada como não encontrada.")
    @ApiResponse(responseCode = "200", description = "Change request encontrada.", content = @Content(schema = @Schema(implementation = ServicePublicationChangeRequestResponseDTO.class)))
    public ServicePublicationChangeRequestResponseDTO findOwn(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ServicePublicationChangeRequestResponseDTO.from(changes.findOwn(id, principal.getUserId()));
    }
}
