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
@Table(name = "storage_media_variants",
        uniqueConstraints = {@UniqueConstraint(name = "uk_storage_variant_code", columnNames = {"media_id", "variant_code"}), @UniqueConstraint(name = "uk_storage_variant_location", columnNames = {"provider", "namespace", "storage_key"}), @UniqueConstraint(name = "uk_storage_variant_target", columnNames = {"media_id", "id"})})
@Getter @Setter
@org.hibernate.annotations.Immutable
@Check(name = "ck_storage_variant_size", constraints = "size_bytes >= 0")
@Check(name = "ck_storage_variant_dimensions", constraints = "(width IS NULL AND height IS NULL) OR (width IS NOT NULL AND height IS NOT NULL AND width > 0 AND height > 0)")
@Check(name = "ck_storage_variant_checksum", constraints = "(checksum_algorithm IS NULL AND checksum IS NULL) OR (checksum_algorithm IS NOT NULL AND checksum IS NOT NULL)")
@Check(name = "ck_storage_variant_master", constraints = "(role = 'MASTER' AND variant_code = 'MASTER') OR (role = 'DERIVED' AND variant_code NOT IN ('MASTER','ORIGINAL'))")
@Check(name = "ck_storage_variant_code_uppercase", constraints = "variant_code = UPPER(variant_code)")
@Check(name = "ck_storage_variant_role", constraints = "role IN ('MASTER','DERIVED')")
public class MediaVariantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long variantId;

    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    @Column(name = "variant_code", nullable = false, length = 64)
    private String variantCode;

    @Convert(converter = com.jeepclub.backend.storage.infra.persistence.mapper.MediaEnumConverters.MediaVariantRoleConverter.class)
    @Column(name = "role", nullable = false, length = 16)
    private MediaVariantRole role;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "namespace", nullable = false, length = 64)
    private String namespace;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "mime_type", nullable = false, length = 127)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "width", nullable = true)
    private Integer width;

    @Column(name = "height", nullable = true)
    private Integer height;

    @Column(name = "checksum_algorithm", nullable = true, length = 32)
    private String checksumAlgorithm;

    @Column(name = "checksum", nullable = true, length = 128)
    private String checksum;

    @Column(name = "preset_code", nullable = true, length = 64)
    private String presetCode;

    @Column(name = "processing_version", nullable = true, length = 64)
    private String processingVersion;

    @Column(name = "transformation_code", nullable = true, length = 64)
    private String transformationCode;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_storage_variant_asset", options = "ON DELETE RESTRICT ON UPDATE RESTRICT"))
    private MediaAssetEntity asset;
}
