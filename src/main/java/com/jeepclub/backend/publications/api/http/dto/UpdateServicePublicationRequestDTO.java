package com.jeepclub.backend.publications.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "PATCH do proprietário cria proposta completa pendente de aprovação. Omitido preserva; null é inválido; galeria enviada substitui a anterior.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record UpdateServicePublicationRequestDTO(
        @Size(max = 200) String title,
        String content,
        @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @Size(max = 30) String contactPhone,
        @Size(min = 1, max = 5) List<@NotNull @Valid PublicationImageRequestDTO> images
) { }
