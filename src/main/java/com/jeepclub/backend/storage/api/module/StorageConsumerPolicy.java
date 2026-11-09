package com.jeepclub.backend.storage.api.module;

import com.jeepclub.backend.storage.core.domain.enums.MediaContext;
import com.jeepclub.backend.storage.core.domain.model.MediaReference;

/** The owning bounded context decides resource state, ownership and historical audience. */
public interface StorageConsumerPolicy {
    MediaContext context();
    boolean mayUpload(long actor, String consumerId);
    boolean mayRead(long actor, MediaReference reference, boolean master);
}
