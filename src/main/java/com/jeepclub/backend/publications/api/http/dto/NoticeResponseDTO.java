package com.jeepclub.backend.publications.api.http.dto;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.model.Notice;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Aviso operacional, inclusive rascunho ou arquivado na superfície administrativa.")
public record NoticeResponseDTO(
        Long id,
        Long authorUserId,
        String title,
        String content,
        @Schema(allowableValues = {"DRAFT", "PUBLISHED", "ARCHIVED"}) PublicationStatus status,
        List<PublicationImageResponseDTO> images,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant archivedAt
) {
    public static NoticeResponseDTO from(Notice notice) {
        return new NoticeResponseDTO(notice.getId(), notice.getAuthorUserId(), notice.getTitle(), notice.getContent(),
                notice.getStatus(), notice.getImages().stream().map(PublicationImageResponseDTO::from).toList(),
                notice.getCreatedAt(), notice.getUpdatedAt(), notice.getPublishedAt(), notice.getArchivedAt());
    }
}
