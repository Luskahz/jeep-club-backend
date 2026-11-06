package com.jeepclub.backend.storage.core.repository;

import com.jeepclub.backend.storage.core.domain.model.*;
import java.util.Optional;

/** Internal persistence port, NOT an authorized consumer API. No bytes, keys lookup or delete operations. */
public interface MediaCatalogRepository {
    Optional<MediaAsset> findAsset(Long mediaId);
    Optional<MediaVariant> findVariant(Long variantId);
    Optional<MediaReference> findReference(Long referenceId);
    MediaAsset registerAsset(MediaAsset asset, MediaAuditContext audit);
    MediaAsset updateAssetMetadata(MediaAsset asset, MediaAuditContext audit);
    MediaVariant registerVariant(MediaVariant variant, MediaAuditContext audit);
    MediaReference recordReference(MediaReference reference, MediaAuditContext audit);
    MediaReference endReference(MediaReference reference, MediaAuditContext audit);
}
