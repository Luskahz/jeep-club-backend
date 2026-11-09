package com.jeepclub.backend.storage.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="storage_upload_permits") @Getter @Setter
public class UploadPermitEntity {
    @Id @Column(name="id",length=36) private String id;
    @Column(name="actor_user_id",nullable=false) private long actorUserId;
    @Column(name="accounted_bytes",nullable=false) private long accountedBytes;
    @Column(name="charged",nullable=false) private boolean charged;
    @Column(name="completed",nullable=false) private boolean completed;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP)
    @Column(name="created_at",nullable=false) private Instant createdAt;
}
