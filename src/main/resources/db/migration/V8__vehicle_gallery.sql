-- BACK-525. Additive rollout: keep legacy photo as a projection of the canonical primary.
-- Stop mixed-version writers before enabling gallery updates; old binaries cannot maintain it.
SET @back525_ddl = (SELECT IF(COUNT(*)=0,'ALTER TABLE vehicles_vehicle ADD COLUMN gallery_revision BIGINT NOT NULL DEFAULT 0','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='vehicles_vehicle' AND column_name='gallery_revision');
PREPARE back525_statement FROM @back525_ddl;
EXECUTE back525_statement;
DEALLOCATE PREPARE back525_statement;
SET @back525_ddl = (SELECT IF(COUNT(*)=0,'ALTER TABLE vehicles_vehicle_history ADD COLUMN gallery_revision BIGINT NOT NULL DEFAULT 0','SELECT 1') FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='vehicles_vehicle_history' AND column_name='gallery_revision');
PREPARE back525_statement FROM @back525_ddl;
EXECUTE back525_statement;
DEALLOCATE PREPARE back525_statement;

CREATE TABLE IF NOT EXISTS vehicle_images (
    vehicle_id BIGINT NOT NULL,
    storage_key VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    position INTEGER NOT NULL,
    is_primary BIT NOT NULL,
    CONSTRAINT uk_vehicle_image_position UNIQUE (vehicle_id,position),
    CONSTRAINT uk_vehicle_image_key UNIQUE (vehicle_id,storage_key),
    CONSTRAINT fk_vehicle_images_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles_vehicle(id),
    CONSTRAINT ck_vehicle_image_position CHECK (position BETWEEN 0 AND 4)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS vehicle_image_history (
    history_id BIGINT NOT NULL,
    storage_key VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    position INTEGER NOT NULL,
    is_primary BIT NOT NULL,
    CONSTRAINT uk_vehicle_history_image_position UNIQUE (history_id,position),
    CONSTRAINT uk_vehicle_history_image_key UNIQUE (history_id,storage_key),
    CONSTRAINT fk_vehicle_image_history_history FOREIGN KEY (history_id) REFERENCES vehicles_vehicle_history(id),
    CONSTRAINT ck_vehicle_history_image_position CHECK (position BETWEEN 0 AND 4)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Preserve opaque legacy values (including URLs). Do not overwrite any populated gallery.
-- New writers clear photo together with an explicit empty gallery, so replay cannot resurrect it.
INSERT INTO vehicle_images(vehicle_id,storage_key,position,is_primary)
SELECT v.id,v.photo,0,b'1' FROM vehicles_vehicle v
WHERE v.photo IS NOT NULL AND NOT EXISTS (SELECT 1 FROM vehicle_images i WHERE i.vehicle_id=v.id);

INSERT INTO vehicle_image_history(history_id,storage_key,position,is_primary)
SELECT h.id,h.photo,0,b'1' FROM vehicles_vehicle_history h
WHERE h.photo IS NOT NULL AND NOT EXISTS (SELECT 1 FROM vehicle_image_history i WHERE i.history_id=h.id);
