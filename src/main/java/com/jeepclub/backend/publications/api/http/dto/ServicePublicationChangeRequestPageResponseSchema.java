package com.jeepclub.backend.publications.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PageResponseAdminServicePublicationChangeRequestResponseDTO", description = "Página estável de propostas de alteração de serviços.")
public record ServicePublicationChangeRequestPageResponseSchema(
        List<AdminServicePublicationChangeRequestResponseDTO> content, int number, int size, long totalElements,
        int totalPages, int numberOfElements, boolean first, boolean last, boolean empty
) { }
