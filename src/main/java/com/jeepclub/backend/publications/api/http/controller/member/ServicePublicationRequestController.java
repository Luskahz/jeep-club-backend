package com.jeepclub.backend.publications.api.http.controller.member;

import com.jeepclub.backend.memberships.api.security.RequiresMembership;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.publications.api.http.dto.*;
import com.jeepclub.backend.publications.core.application.service.ServicePublicationRequestService;
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
@RequestMapping("/service-publication-requests")
@RequiresMembership
@RequiredArgsConstructor
@Tag(name = "Publications - Service Requests", description = "Solicitações iniciais de serviços feitas por membros ativos.")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Payload ou galeria inválida.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "402", description = "Membership ativa exigida.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permission ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Request ou mídia não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class ServicePublicationRequestController {
    private final ServicePublicationRequestService requests;
    private final ServiceRequestReader reader;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_REQUEST_CREATE')")
    @RequiredPermission("PUBLICATIONS_SERVICE_REQUEST_CREATE")
    @Operation(summary = "Solicitar publicação de serviço", description = "O requester vem do principal; a request nasce PENDING e não é uma Publication.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = CreateServicePublicationRequestDTO.class))))
    @ApiResponse(responseCode = "201", description = "Request PENDING criada.", content = @Content(schema = @Schema(implementation = ServicePublicationRequestResponseDTO.class)))
    public ResponseEntity<ServicePublicationRequestResponseDTO> create(@RequestBody JsonNode body,
            @AuthenticationPrincipal UserPrincipal principal) {
        var request = reader.create(body);
        var images = request.images().stream().map(PublicationImageRequestDTO::toDomain).toList();
        return ResponseEntity.status(201).body(ServicePublicationRequestResponseDTO.from(requests.create(
                principal.getUserId(), request.title(), request.content(), request.amount(), request.contactPhone(), images)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_REQUEST_READ')")
    @RequiredPermission("PUBLICATIONS_SERVICE_REQUEST_READ")
    @Operation(summary = "Consultar solicitação inicial própria", description = "Solicitações de outros usuários são tratadas como não encontradas.")
    @ApiResponse(responseCode = "200", description = "Request encontrada.", content = @Content(schema = @Schema(implementation = ServicePublicationRequestResponseDTO.class)))
    public ServicePublicationRequestResponseDTO findOwn(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ServicePublicationRequestResponseDTO.from(requests.findOwn(id, principal.getUserId()));
    }
}
