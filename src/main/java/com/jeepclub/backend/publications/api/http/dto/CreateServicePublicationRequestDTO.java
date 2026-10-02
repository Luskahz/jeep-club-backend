package com.jeepclub.backend.publications.api.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Solicitação inicial de publicação de serviço. Autor, status e revisão são definidos pelo servidor.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CreateServicePublicationRequestDTO(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 30) String contactPhone,
        @NotNull @Size(min = 1, max = 5) List<@NotNull @Valid PublicationImageRequestDTO> images
) { }
