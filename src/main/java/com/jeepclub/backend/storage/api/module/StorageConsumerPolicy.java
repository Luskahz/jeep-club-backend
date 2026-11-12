package com.jeepclub.backend.storage.api.module;

import com.jeepclub.backend.storage.core.domain.enums.MediaContext;
import com.jeepclub.backend.storage.core.domain.model.MediaReference;
import com.jeepclub.backend.storage.core.domain.model.MediaAsset;
import com.jeepclub.backend.storage.core.domain.enums.MediaSubjectType;

/** The owning bounded context decides resource state, ownership and historical audience. */
public interface StorageConsumerPolicy {
    MediaContext context();
    boolean mayUpload(long actor, String consumerId);
    /** Provenance is supplied by the owning context, never inferred from the acting user. */
    record UploadIdentity(Long ownerUserId, MediaSubjectType subjectType, Long subjectId) {}
    default UploadIdentity uploadIdentity(long actor, String consumerId) {
        return new UploadIdentity(null, MediaSubjectType.UNKNOWN, null);
    }
    /** Separate permission for attaching an existing asset, including explicitly authorized sharing. */
    default boolean mayAssociate(long actor, MediaAsset asset, String consumerId, String role) {
        return false;
    }
    default boolean mayDissociate(long actor, MediaReference reference) { return false; }
    boolean mayRead(long actor, MediaReference reference, boolean master);
}
