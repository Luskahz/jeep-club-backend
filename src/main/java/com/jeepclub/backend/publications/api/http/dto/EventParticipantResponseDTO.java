package com.jeepclub.backend.publications.api.http.dto;

import com.jeepclub.backend.publications.core.application.service.EventService;

public record EventParticipantResponseDTO(Long userId, String name, String profilePhotoStorageKey) {
    public static EventParticipantResponseDTO from(EventService.Participant participant) {
        return new EventParticipantResponseDTO(participant.userId(), participant.name(), participant.profilePhotoStorageKey());
    }
}
