package com.jeepclub.backend.storage.core.repository;

import com.jeepclub.backend.storage.core.domain.model.MediaAuditContext;

/** Internal maintenance entry point; apply must remain disabled until separately activated. */
public interface LegacyBackfill {
    record Result(String state,Long mediaId,long sizeBytes) {}
    Result inspect(String legacyKey);
    Result apply(String legacyKey,MediaAuditContext actor);
    void rollback(String legacyKey,MediaAuditContext actor);
}
