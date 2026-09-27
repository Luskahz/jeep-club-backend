package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "publication_event_history")
@PrimaryKeyJoinColumn(name = "history_id")
@Getter
@Setter
public class EventHistoryEntity extends PublicationHistoryEntity {
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;
    private Instant endsAt;
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private com.jeepclub.backend.publications.core.domain.enums.EventStatus eventStatus;
}
