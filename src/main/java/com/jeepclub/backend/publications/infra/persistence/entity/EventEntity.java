package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "publication_events")
@PrimaryKeyJoinColumn(name = "publication_id")
@Getter
@Setter
public class EventEntity extends PublicationEntity {
    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;
    private Instant endsAt;
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("'OPEN'")
    private com.jeepclub.backend.publications.core.domain.enums.EventStatus eventStatus = com.jeepclub.backend.publications.core.domain.enums.EventStatus.OPEN;
}
