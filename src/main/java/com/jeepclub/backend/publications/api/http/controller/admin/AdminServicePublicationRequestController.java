package com.jeepclub.backend.publications.api.http.controller.admin;

import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.publications.api.http.dto.*;
import com.jeepclub.backend.publications.core.application.service.AdminServicePublicationRequestService;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/service-publication-requests")
@RequiredArgsConstructor
@Tag(name = "Publications - Service Request Admin", description = "Revisão administrativa de solicitações iniciais.")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Payload ou filtro inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permission ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Request não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Request já processada ou conflito concorrente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AdminServicePublicationRequestController {
    private final AdminServicePublicationRequestService requests;

    @GetMapping
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_REQUEST_ADMIN_READ')")
    @RequiredPermission("PUBLICATIONS_SERVICE_REQUEST_ADMIN_READ")
    @Operation(summary = "Listar solicitações iniciais", description = "Página administrativa, com filtro opcional por PENDING, APPROVED ou REJECTED.")
    @ApiResponse(responseCode = "200", description = "Página de requests.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ServicePublicationRequestPageResponseSchema.class)))
    public PageResponse<AdminServicePublicationRequestResponseDTO> list(
            @RequestParam(required = false) ServicePublicationRequestStatus status,
            @PageableDefault(size = 20, sort = {"requestedAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(requests.findAll(status, pageable).map(AdminServicePublicationRequestResponseDTO::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_REQUEST_ADMIN_READ')")
    @RequiredPermission("PUBLICATIONS_SERVICE_REQUEST_ADMIN_READ")
    @Operation(summary = "Consultar solicitação inicial administrativamente")
    @ApiResponse(responseCode = "200", description = "Request encontrada.", content = @Content(schema = @Schema(implementation = AdminServicePublicationRequestResponseDTO.class)))
    public AdminServicePublicationRequestResponseDTO find(@PathVariable Long id) {
        return AdminServicePublicationRequestResponseDTO.from(requests.findById(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_REQUEST_APPROVE')")
    @RequiredPermission("PUBLICATIONS_SERVICE_REQUEST_APPROVE")
    @Operation(summary = "Aprovar solicitação inicial", description = "Sob lock, cria ServicePublication PUBLISHED e registra createdPublicationId na request; commit único.")
    @ApiResponse(responseCode = "200", description = "Request aprovada e Service criado.", content = @Content(schema = @Schema(implementation = AdminServicePublicationRequestResponseDTO.class)))
    public AdminServicePublicationRequestResponseDTO approve(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return AdminServicePublicationRequestResponseDTO.from(requests.approve(id, principal.getUserId()));
    }

    @PostMapping(value = "/{id}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_REQUEST_REJECT')")
    @RequiredPermission("PUBLICATIONS_SERVICE_REQUEST_REJECT")
    @Operation(summary = "Rejeitar solicitação inicial", description = "Somente PENDING pode ser rejeitada; não cria Service.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    content = @Content(schema = @Schema(implementation = RejectServiceRequestDTO.class))))
    @ApiResponse(responseCode = "200", description = "Request rejeitada.", content = @Content(schema = @Schema(implementation = AdminServicePublicationRequestResponseDTO.class)))
    public AdminServicePublicationRequestResponseDTO reject(@PathVariable Long id,
            @RequestBody(required = false) RejectServiceRequestDTO body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return AdminServicePublicationRequestResponseDTO.from(requests.reject(id, principal.getUserId(),
                body == null ? null : body.rejectionReason()));
    }
}
