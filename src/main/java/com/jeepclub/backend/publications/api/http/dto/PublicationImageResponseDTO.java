package com.jeepclub.backend.publications.api.http.dto;

import com.jeepclub.backend.publications.core.domain.model.PublicationImage;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Imagem associada ao aviso. GET /media/images?key=... resolve a storageKey.")
public record PublicationImageResponseDTO(
        @Schema(description = "Chave da imagem no storage global.") String storageKey,
        @Schema(description = "Posição na galeria, a partir de zero.") int position,
        @Schema(description = "Imagem principal da galeria.") boolean primary
) {
    public static PublicationImageResponseDTO from(PublicationImage image) {
        return new PublicationImageResponseDTO(image.storageKey(), image.position(), image.primary());
    }
}
