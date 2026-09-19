package com.jeepclub.backend.platform.logging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "platform_logs", indexes = {
        @Index(name = "idx_platform_logs_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_platform_logs_actor_id", columnList = "actor_id"),
        @Index(name = "idx_platform_logs_action", columnList = "action")
})
@Getter
@NoArgsConstructor
public class SystemLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(nullable = false)
    private int status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SystemLogOutcome outcome;

    @Column(name = "duration_millis", nullable = false)
    private long durationMillis;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public SystemLogEntity(SystemLogEvent event) {
        this.actorId = event.actorId();
        this.action = event.action();
        this.method = event.method();
        this.path = event.path();
        this.status = event.status();
        this.outcome = event.outcome();
        this.durationMillis = event.durationMillis();
        this.requestId = event.requestId();
        this.occurredAt = event.occurredAt();
    }
}
