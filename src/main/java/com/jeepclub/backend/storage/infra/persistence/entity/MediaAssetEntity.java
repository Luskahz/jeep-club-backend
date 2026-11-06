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
@Table(name = "storage_media_assets",
        indexes = {@Index(name = "ix_storage_asset_uploader", columnList = "uploaded_by_user_id,id"), @Index(name = "ix_storage_asset_subject", columnList = "subject_type,subject_id,id"), @Index(name = "ix_storage_asset_owner", columnList = "owner_user_id,id"), @Index(name = "ix_storage_asset_classification", columnList = "classification,id")})
@Getter @Setter
@Check(name = "ck_storage_asset_subject", constraints = "(subject_type = 'UNKNOWN' AND subject_id IS NULL) OR (subject_type IN ('USER','DEPENDENT') AND subject_id IS NOT NULL AND subject_id > 0)")
@Check(name = "ck_storage_asset_dates", constraints = "updated_at >= created_at")
@Check(name = "ck_storage_asset_version", constraints = "version >= 0")
@Check(name = "ck_storage_asset_uploader", constraints = "uploaded_by_user_id IS NULL OR uploaded_by_user_id > 0")
@Check(name = "ck_storage_asset_owner", constraints = "owner_user_id IS NULL OR owner_user_id > 0")
@Check(name = "ck_storage_asset_origin", constraints = "origin IN ('UNKNOWN','UPLOAD','LEGACY_IMPORT')")
@Check(name = "ck_storage_asset_source_context", constraints = "source_context IN ('UNKNOWN','IDENTITY_PROFILE','VEHICLE_PHOTO','PUBLICATION_IMAGE','PUBLICATION_COMMENT','EVENT_IMAGE','TOOL_IMAGE','DEPENDENT_PHOTO')")
@Check(name = "ck_storage_asset_subject_type", constraints = "subject_type IN ('UNKNOWN','USER','DEPENDENT')")
@Check(name = "ck_storage_asset_classification", constraints = "classification IN ('UNCLASSIFIED','CLASSIFIED')")
@Check(name = "ck_storage_asset_physical_state", constraints = "physical_state IN ('UNKNOWN','PRESENT','MISSING')")
public class MediaAssetEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long mediaId;

    @Convert(converter = com.jeepclub.backend.storage.infra.persistence.mapper.MediaEnumConverters.MediaOriginConverter.class)
    @Column(name = "origin", nullable = false, length = 24)
    private MediaOrigin origin;

    @Convert(converter = com.jeepclub.backend.storage.infra.persistence.mapper.MediaEnumConverters.MediaContextConverter.class)
    @Column(name = "source_context", nullable = false, length = 32)
    private MediaContext sourceContext;

    @Column(name = "source_record_id", nullable = true, length = 128)
    private String sourceRecordId;

    @Column(name = "uploaded_by_user_id", nullable = true)
    private Long uploadedByUserId;

    @Column(name = "owner_user_id", nullable = true)
    private Long ownerUserId;

    @Convert(converter = com.jeepclub.backend.storage.infra.persistence.mapper.MediaEnumConverters.MediaSubjectTypeConverter.class)
    @Column(name = "subject_type", nullable = false, length = 16)
    private MediaSubjectType subjectType;

    @Column(name = "subject_id", nullable = true)
    private Long subjectId;

    @Column(name = "purpose", nullable = false, length = 64)
    private String purpose;

    @Convert(converter = com.jeepclub.backend.storage.infra.persistence.mapper.MediaEnumConverters.MediaClassificationConverter.class)
    @Column(name = "classification", nullable = false, length = 24)
    private MediaClassification classification;

    @Convert(converter = com.jeepclub.backend.storage.infra.persistence.mapper.MediaEnumConverters.MediaPhysicalStateConverter.class)
    @Column(name = "physical_state", nullable = false, length = 16)
    private MediaPhysicalState physicalState;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
