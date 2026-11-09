CREATE TABLE storage_legacy_checkpoints (
 source_id VARCHAR(64) NOT NULL PRIMARY KEY,
 legacy_key VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
 media_id BIGINT NULL, rolled_back BOOLEAN NOT NULL,
 CONSTRAINT fk_storage_checkpoint_asset FOREIGN KEY (media_id) REFERENCES storage_media_assets(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB;
