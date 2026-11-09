-- Preserve V13 metadata and all legacy locations. No data rewrite or physical deletion.
CREATE TABLE storage_blobs (
 sha256 VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 size_bytes BIGINT NOT NULL,
 ready BOOLEAN NOT NULL,
 created_at DATETIME(6) NOT NULL,
 PRIMARY KEY (sha256),
 CONSTRAINT ck_storage_blob_size CHECK (size_bytes > 0 AND size_bytes <= 10485760)
) ENGINE=InnoDB;
CREATE TABLE storage_variant_blobs (
 variant_id BIGINT NOT NULL,
 sha256 VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 PRIMARY KEY (variant_id),
 INDEX ix_storage_binding_blob (sha256),
 CONSTRAINT fk_storage_binding_variant FOREIGN KEY (variant_id) REFERENCES storage_media_variants(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_storage_binding_blob FOREIGN KEY (sha256) REFERENCES storage_blobs(sha256) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;
ALTER TABLE storage_media_variants DROP INDEX uk_storage_variant_location;
CREATE INDEX ix_storage_variant_location ON storage_media_variants(provider, namespace, storage_key);
ALTER TABLE storage_media_variants ADD COLUMN legacy_storage_key VARCHAR(512)
 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin GENERATED ALWAYS AS
 (CASE WHEN namespace = 'storage-blobs' THEN NULL ELSE storage_key END) STORED;
CREATE UNIQUE INDEX uk_storage_legacy_location ON storage_media_variants(provider,namespace,legacy_storage_key);
