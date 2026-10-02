package com.jeepclub.backend.publications.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PageResponseAdminServicePublicationRequestResponseDTO", description = "Página estável de solicitações iniciais de serviços.")
public record ServicePublicationRequestPageResponseSchema(
        List<AdminServicePublicationRequestResponseDTO> content, int number, int size, long totalElements,
        int totalPages, int numberOfElements, boolean first, boolean last, boolean empty
) { }
