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
@Table(name = "storage_media_references",
        uniqueConstraints = {@UniqueConstraint(name = "uk_storage_reference_target", columnNames = {"media_id", "id"})},
        indexes = {@Index(name = "ix_storage_reference_consumer", columnList = "context,consumer_id,unlinked_at,id"), @Index(name = "ix_storage_reference_asset", columnList = "media_id,unlinked_at,id")})
@Getter @Setter
@Check(name = "ck_storage_reference_dates", constraints = "unlinked_at IS NULL OR unlinked_at >= linked_at")
@Check(name = "ck_storage_reference_end", constraints = "unlinked_at IS NOT NULL OR (unlinked_by_user_id IS NULL AND unlink_reason_code IS NULL)")
@Check(name = "ck_storage_reference_position", constraints = "position IS NULL OR position >= 0")
@Check(name = "ck_storage_reference_version", constraints = "version >= 0")
@Check(name = "ck_storage_reference_known_context", constraints = "context <> 'UNKNOWN'")
@Check(name = "ck_storage_reference_link_actor", constraints = "linked_by_user_id IS NULL OR linked_by_user_id > 0")
@Check(name = "ck_storage_reference_unlink_actor", constraints = "unlinked_by_user_id IS NULL OR unlinked_by_user_id > 0")
@Check(name = "ck_storage_reference_context", constraints = "context IN ('UNKNOWN','IDENTITY_PROFILE','VEHICLE_PHOTO','PUBLICATION_IMAGE','PUBLICATION_COMMENT','EVENT_IMAGE','TOOL_IMAGE','DEPENDENT_PHOTO','MEDICAL_IMAGE','IDENTIFICATION_PHOTO')")
@Check(name = "ck_storage_reference_association_origin", constraints = "association_origin IN ('UNKNOWN','UPLOAD','LEGACY_IMPORT')")
public class MediaReferenceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long referenceId;

    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    @Convert(converter = com.jeepclub.backend.storage.infra.persistence.mapper.MediaEnumConverters.MediaContextConverter.class)
    @Column(name = "context", nullable = false, length = 32)
    private MediaContext context;

    @Column(name = "consumer_id", nullable = false, length = 128)
    private String consumerId;

    @Column(name = "role", nullable = false, length = 64)
    private String role;

    @Column(name = "position", nullable = true)
    private Integer position;

    @Convert(converter = com.jeepclub.backend.storage.infra.persistence.mapper.MediaEnumConverters.MediaOriginConverter.class)
    @Column(name = "association_origin", nullable = false, length = 24)
    private MediaOrigin associationOrigin;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "linked_by_user_id", nullable = true)
    private Long linkedByUserId;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "unlinked_at", nullable = true)
    private Instant unlinkedAt;

    @Column(name = "unlinked_by_user_id", nullable = true)
    private Long unlinkedByUserId;

    @Column(name = "unlink_reason_code", nullable = true, length = 64)
    private String unlinkReasonCode;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_storage_reference_asset", options = "ON DELETE RESTRICT ON UPDATE RESTRICT"))
    private MediaAssetEntity asset;
}
