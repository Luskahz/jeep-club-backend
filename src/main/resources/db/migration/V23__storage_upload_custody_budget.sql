CREATE TABLE storage_upload_budgets (
 scope_id BIGINT NOT NULL PRIMARY KEY, used_bytes BIGINT NOT NULL,
 in_flight INT NOT NULL, window_attempts INT NOT NULL, charged_uploads BIGINT NOT NULL,
 window_start DATETIME(6) NOT NULL,
 CONSTRAINT ck_storage_upload_budget CHECK (used_bytes>=0 AND in_flight>=0 AND window_attempts>=0 AND charged_uploads>=0)
) ENGINE=InnoDB;
CREATE TABLE storage_upload_permits (
 id VARCHAR(36) NOT NULL PRIMARY KEY, actor_user_id BIGINT NOT NULL,
 accounted_bytes BIGINT NOT NULL, charged BOOLEAN NOT NULL, completed BOOLEAN NOT NULL,
 created_at DATETIME(6) NOT NULL,
 INDEX ix_storage_upload_actor (actor_user_id,completed),
 CONSTRAINT ck_storage_upload_permit CHECK (actor_user_id>0 AND accounted_bytes>=0)
) ENGINE=InnoDB;
