package com.jeepclub.backend.publications.api.http.controller.admin;

import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.publications.api.http.dto.*;
import com.jeepclub.backend.publications.core.application.service.AdminServicePublicationChangeRequestService;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus;
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
@RequestMapping("/admin/service-publication-change-requests")
@RequiredArgsConstructor
@Tag(name = "Publications - Service Change Admin", description = "Revisão administrativa das alterações propostas.")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Payload ou filtro inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permission ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Change request ou Service alvo não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Change request processada ou estado concorrente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AdminServicePublicationChangeRequestController {
    private final AdminServicePublicationChangeRequestService changes;

    @GetMapping
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_CHANGE_REQUEST_ADMIN_READ')")
    @RequiredPermission("PUBLICATIONS_SERVICE_CHANGE_REQUEST_ADMIN_READ")
    @Operation(summary = "Listar solicitações de alteração", description = "Página administrativa com filtro opcional por status.")
    @ApiResponse(responseCode = "200", description = "Página de change requests.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ServicePublicationChangeRequestPageResponseSchema.class)))
    public PageResponse<AdminServicePublicationChangeRequestResponseDTO> list(
            @RequestParam(required = false) ServicePublicationChangeRequestStatus status,
            @PageableDefault(size = 20, sort = {"requestedAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(changes.findAll(status, pageable).map(AdminServicePublicationChangeRequestResponseDTO::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_CHANGE_REQUEST_ADMIN_READ')")
    @RequiredPermission("PUBLICATIONS_SERVICE_CHANGE_REQUEST_ADMIN_READ")
    @Operation(summary = "Consultar alteração administrativamente")
    @ApiResponse(responseCode = "200", description = "Change request encontrada.", content = @Content(schema = @Schema(implementation = AdminServicePublicationChangeRequestResponseDTO.class)))
    public AdminServicePublicationChangeRequestResponseDTO find(@PathVariable Long id) {
        return AdminServicePublicationChangeRequestResponseDTO.from(changes.findById(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_CHANGE_REQUEST_APPROVE')")
    @RequiredPermission("PUBLICATIONS_SERVICE_CHANGE_REQUEST_APPROVE")
    @Operation(summary = "Aprovar alteração", description = "Aplica o snapshot ao mesmo Service sob lock, preserva publicationId, publishedAt e interações; commit único.")
    @ApiResponse(responseCode = "200", description = "Alteração aprovada e aplicada.", content = @Content(schema = @Schema(implementation = AdminServicePublicationChangeRequestResponseDTO.class)))
    public AdminServicePublicationChangeRequestResponseDTO approve(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return AdminServicePublicationChangeRequestResponseDTO.from(changes.approve(id, principal.getUserId()));
    }

    @PostMapping(value = "/{id}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PUBLICATIONS_SERVICE_CHANGE_REQUEST_REJECT')")
    @RequiredPermission("PUBLICATIONS_SERVICE_CHANGE_REQUEST_REJECT")
    @Operation(summary = "Rejeitar alteração", description = "Service publicado permanece inalterado; revisor e motivo ficam na change request.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    content = @Content(schema = @Schema(implementation = RejectServiceRequestDTO.class))))
    @ApiResponse(responseCode = "200", description = "Alteração rejeitada.", content = @Content(schema = @Schema(implementation = AdminServicePublicationChangeRequestResponseDTO.class)))
    public AdminServicePublicationChangeRequestResponseDTO reject(@PathVariable Long id,
            @RequestBody(required = false) RejectServiceRequestDTO body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return AdminServicePublicationChangeRequestResponseDTO.from(changes.reject(id, principal.getUserId(),
                body == null ? null : body.rejectionReason()));
    }
}
