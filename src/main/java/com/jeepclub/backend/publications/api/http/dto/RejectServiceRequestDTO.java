package com.jeepclub.backend.publications.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Motivo opcional da rejeição; texto vazio é normalizado para null.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record RejectServiceRequestDTO(String rejectionReason) { }
