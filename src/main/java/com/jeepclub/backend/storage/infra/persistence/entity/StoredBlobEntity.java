package com.jeepclub.backend.storage.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name = "storage_blobs") @Getter @Setter
public class StoredBlobEntity {
    @Id @Column(name="sha256", length=64) private String sha256;
    @Column(name="size_bytes", nullable=false) private long sizeBytes;
    @Column(name="ready", nullable=false) private boolean ready;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP)
    @Column(name="created_at", nullable=false) private Instant createdAt;
}
