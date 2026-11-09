CREATE TABLE storage_media_control (
 media_id BIGINT NOT NULL PRIMARY KEY, received_sha256 VARCHAR(64) NOT NULL,
 state VARCHAR(24) NOT NULL, retention_sensitive BOOLEAN NOT NULL, legal_hold BOOLEAN NOT NULL,
 version BIGINT NOT NULL,
 CONSTRAINT fk_storage_control_asset FOREIGN KEY (media_id) REFERENCES storage_media_assets(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;
CREATE TABLE storage_governance_events (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, media_id BIGINT NOT NULL, reference_id BIGINT NULL,
 operation VARCHAR(32) NOT NULL, state VARCHAR(24) NULL, reason VARCHAR(32) NULL, internal_note VARCHAR(2000) NULL,
 actor_user_id BIGINT NULL, occurred_at DATETIME(6) NOT NULL,
 INDEX ix_storage_governance_media (media_id,id),
 CONSTRAINT fk_storage_governance_asset FOREIGN KEY (media_id) REFERENCES storage_media_assets(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
 CONSTRAINT fk_storage_governance_reference FOREIGN KEY (media_id,reference_id) REFERENCES storage_media_references(media_id,id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;
