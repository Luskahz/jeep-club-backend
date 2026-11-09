package com.jeepclub.backend.storage.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="storage_upload_budgets") @Getter @Setter
public class UploadBudgetEntity {
    @Id @Column(name="scope_id") private Long scopeId;
    @Column(name="used_bytes",nullable=false) private long usedBytes;
    @Column(name="in_flight",nullable=false) private int inFlight;
    @Column(name="window_attempts",nullable=false) private int windowAttempts;
    @Column(name="charged_uploads",nullable=false) private long chargedUploads;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP)
    @Column(name="window_start",nullable=false) private Instant windowStart;
}
