package com.jeepclub.backend.storage.infra.persistence.entity;

import com.jeepclub.backend.storage.core.domain.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "storage_media_audit_events",
        indexes = {@Index(name = "ix_storage_audit_asset", columnList = "media_id,occurred_at,id")})
@Getter @Setter
@org.hibernate.annotations.Immutable
@Check(name = "ck_storage_audit_actor", constraints = "(actor_kind = 'USER' AND actor_user_id IS NOT NULL AND actor_user_id > 0) OR (actor_kind IN ('UNKNOWN','SYSTEM') AND actor_user_id IS NULL)")
@Check(name = "ck_storage_audit_target", constraints = "variant_id IS NULL OR reference_id IS NULL")
@Check(name = "ck_storage_audit_versions", constraints = "(previous_version IS NULL OR previous_version >= 0) AND (resulting_version IS NULL OR resulting_version >= 0)")
@Check(name = "ck_storage_auditevent_operation", constraints = "operation IN ('ASSET_REGISTERED','ASSET_METADATA_UPDATED','VARIANT_REGISTERED','REFERENCE_RECORDED','REFERENCE_ENDED')")
@Check(name = "ck_storage_auditevent_actor_kind", constraints = "actor_kind IN ('UNKNOWN','USER','SYSTEM')")
public class MediaAuditEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long eventId;

    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    @Column(name = "variant_id", nullable = true)
    private Long variantId;

    @Column(name = "reference_id", nullable = true)
    private Long referenceId;

    @Convert(converter = com.jeepclub.backend.storage.infra.persistence.mapper.MediaEnumConverters.MediaAuditOperationConverter.class)
    @Column(name = "operation", nullable = false, length = 32)
    private MediaAuditOperation operation;

    @Convert(converter = com.jeepclub.backend.storage.infra.persistence.mapper.MediaEnumConverters.MediaActorKindConverter.class)
    @Column(name = "actor_kind", nullable = false, length = 16)
    private MediaActorKind actorKind;

    @Column(name = "actor_user_id", nullable = true)
    private Long actorUserId;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "correlation_id", nullable = true, length = 64)
    private String correlationId;

    @Column(name = "previous_version", nullable = true)
    private Long previousVersion;

    @Column(name = "resulting_version", nullable = true)
    private Long resultingVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_storage_auditevent_asset"))
    private MediaAssetEntity asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(value = {
        @JoinColumn(name = "media_id", referencedColumnName = "media_id", insertable = false, updatable = false),
        @JoinColumn(name = "variant_id", referencedColumnName = "id", insertable = false, updatable = false)
    }, foreignKey = @ForeignKey(name = "fk_storage_audit_variant"))
    private MediaVariantEntity variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(value = {
        @JoinColumn(name = "media_id", referencedColumnName = "media_id", insertable = false, updatable = false),
        @JoinColumn(name = "reference_id", referencedColumnName = "id", insertable = false, updatable = false)
    }, foreignKey = @ForeignKey(name = "fk_storage_audit_reference"))
    private MediaReferenceEntity reference;
}
