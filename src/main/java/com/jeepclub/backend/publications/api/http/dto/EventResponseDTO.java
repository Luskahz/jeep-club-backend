package com.jeepclub.backend.publications.api.http.dto;

import com.jeepclub.backend.publications.core.domain.model.Event;
import com.jeepclub.backend.publications.core.domain.enums.*;
import java.time.Instant;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public record EventResponseDTO(Long id, Long authorUserId, String title, String content, PublicationStatus publicationStatus,
        @Schema(description="OPEN before startsAt, IN_PROGRESS from startsAt, FINISHED at endsAt or explicit finish, CANCELLED by command. Independent from editorial status.") EventStatus eventStatus,
        Instant startsAt, Instant endsAt, List<PublicationImageResponseDTO> images) {
    public static EventResponseDTO from(Event e, Instant now) {
        return new EventResponseDTO(e.getId(), e.getAuthorUserId(), e.getTitle(), e.getContent(), e.getStatus(), e.effectiveStatus(now),
            e.getStartsAt(), e.getEndsAt(), e.getImages().stream().map(i -> new PublicationImageResponseDTO(i.storageKey(), i.position(), i.primary())).toList());
    }
}
