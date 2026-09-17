package com.jeepclub.backend.platform.web.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Objects;

@Schema(description = "Página estável de recursos da API do Jeep Club.")
public record PageResponse<T>(
        @Schema(description = "Recursos presentes na página.")
        List<T> content,
        @Schema(description = "Índice zero-based da página retornada.", example = "0")
        int number,
        @Schema(description = "Tamanho solicitado da página.", example = "20")
        int size,
        @Schema(description = "Quantidade total de elementos elegíveis.", example = "42")
        long totalElements,
        @Schema(description = "Quantidade total de páginas.", example = "3")
        int totalPages,
        @Schema(description = "Quantidade de elementos nesta página.", example = "20")
        int numberOfElements,
        @Schema(description = "Indica se esta é a primeira página.", example = "true")
        boolean first,
        @Schema(description = "Indica se esta é a última página.", example = "false")
        boolean last,
        @Schema(description = "Indica se a página está vazia.", example = "false")
        boolean empty
) {

    public PageResponse {
        content = List.copyOf(Objects.requireNonNull(content, "content must not be null"));
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        Objects.requireNonNull(page, "page must not be null");

        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumberOfElements(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }
}
