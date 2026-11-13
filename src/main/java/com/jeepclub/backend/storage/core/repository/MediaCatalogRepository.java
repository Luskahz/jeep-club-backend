package com.jeepclub.backend.storage.core.repository;

import com.jeepclub.backend.storage.core.domain.model.*;
import java.util.Optional;
import java.util.List;

/** Internal persistence port, NOT an authorized consumer API. No bytes, keys lookup or delete operations. */
public interface MediaCatalogRepository {
    /** Lock affected assets in ascending order after the consumer aggregate lock. */
    void lockAssets(List<Long> mediaIds);
    Optional<MediaAsset> findAsset(Long mediaId);
    Optional<MediaVariant> findVariant(Long variantId);
    Optional<MediaVariant> findVariant(Long mediaId, String code);
    Optional<MediaReference> findReference(Long referenceId);
    List<MediaReference> findReferences(Long mediaId, long afterId, int limit);
    List<MediaAuditEvent> findAuditEvents(Long mediaId, long afterId, int limit);
    MediaAsset registerAsset(MediaAsset asset, MediaAuditContext audit);
    MediaAsset updateAssetMetadata(MediaAsset asset, MediaAuditContext audit);
    /** Classify only a catalogued representation whose bytes were actually read and verified. */
    MediaAsset confirmPhysicalPresence(Long mediaId, Long expectedVersion, Long variantId, byte[] verifiedBytes, MediaAuditContext audit);
    MediaVariant registerVariant(MediaVariant variant, MediaAuditContext audit);
    MediaReference recordReference(MediaReference reference, MediaAuditContext audit);
    MediaReference endReference(MediaReference reference, MediaAuditContext audit);
}
