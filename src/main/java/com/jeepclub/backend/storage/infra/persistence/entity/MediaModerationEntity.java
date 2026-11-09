package com.jeepclub.backend.storage.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** Append-only operational audit, including scoped moderation decisions. */
@Entity @Table(name="storage_governance_events",indexes=@Index(name="ix_storage_governance_media",columnList="media_id,id")) @Getter @Setter
@org.hibernate.annotations.Immutable
public class MediaModerationEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="media_id",nullable=false) private Long mediaId;
    @Column(name="reference_id") private Long referenceId;
    @Column(name="operation",nullable=false,length=32) private String operation;
    @Column(name="state",length=24) private String state;
    @Column(name="reason",length=32) private String reason;
    @Column(name="internal_note",length=2000) private String internalNote;
    @Column(name="actor_user_id") private Long actorUserId;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.TIMESTAMP)
    @Column(name="occurred_at",nullable=false) private Instant occurredAt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="media_id",insertable=false,updatable=false,
        foreignKey=@ForeignKey(name="fk_storage_governance_asset",options="ON DELETE RESTRICT ON UPDATE RESTRICT"))
    private MediaAssetEntity asset;
}
