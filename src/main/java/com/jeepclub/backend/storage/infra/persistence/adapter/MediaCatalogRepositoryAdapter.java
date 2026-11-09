package com.jeepclub.backend.storage.infra.persistence.adapter;

import com.jeepclub.backend.storage.core.domain.enums.MediaAuditOperation;
import com.jeepclub.backend.storage.core.domain.model.*;
import com.jeepclub.backend.storage.core.repository.MediaCatalogRepository;
import com.jeepclub.backend.storage.infra.persistence.entity.*;
import com.jeepclub.backend.storage.infra.persistence.mapper.MediaCatalogMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.List;
import java.util.Optional;

/** Internal catalog adapter. Each write and its immutable event commit or roll back together. */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaCatalogRepositoryAdapter implements MediaCatalogRepository {
    private final EntityManager entityManager;

    @Override public Optional<MediaAsset> findAsset(Long id) {
        return Optional.ofNullable(entityManager.find(MediaAssetEntity.class, id)).map(MediaCatalogMapper::toDomain);
    }
    @Override public Optional<MediaVariant> findVariant(Long id) {
        return Optional.ofNullable(entityManager.find(MediaVariantEntity.class, id)).map(MediaCatalogMapper::toDomain);
    }
    @Override public Optional<MediaVariant> findVariant(Long mediaId, String code) {
        return entityManager.createQuery("select v from MediaVariantEntity v where v.mediaId=:media and v.variantCode=:code",MediaVariantEntity.class)
            .setParameter("media",mediaId).setParameter("code",code).getResultStream().findFirst().map(MediaCatalogMapper::toDomain);
    }
    @Override public Optional<MediaReference> findReference(Long id) {
        return Optional.ofNullable(entityManager.find(MediaReferenceEntity.class, id)).map(MediaCatalogMapper::toDomain);
    }

    @Override public List<MediaReference> findReferences(Long mediaId, long afterId, int limit) {
        validatePage(mediaId, afterId, limit);
        return entityManager.createQuery("select r from MediaReferenceEntity r where r.mediaId = :mediaId and r.referenceId > :afterId order by r.referenceId", MediaReferenceEntity.class)
                .setParameter("mediaId", mediaId).setParameter("afterId", afterId).setMaxResults(limit)
                .getResultList().stream().map(MediaCatalogMapper::toDomain).toList();
    }

    @Override public List<MediaAuditEvent> findAuditEvents(Long mediaId, long afterId, int limit) {
        validatePage(mediaId, afterId, limit);
        return entityManager.createQuery("select e from MediaAuditEventEntity e where e.mediaId = :mediaId and e.eventId > :afterId order by e.eventId", MediaAuditEventEntity.class)
                .setParameter("mediaId", mediaId).setParameter("afterId", afterId).setMaxResults(limit)
                .getResultList().stream().map(MediaCatalogMapper::toDomain).toList();
    }

    private static void validatePage(Long mediaId, long afterId, int limit) {
        require(mediaId != null && mediaId > 0 && afterId >= 0 && limit > 0 && limit <= 100, "Invalid catalog page");
    }

    @Override @Transactional
    public MediaAsset registerAsset(MediaAsset asset, MediaAuditContext audit) {
        require(asset.mediaId() == null, "Asset already registered");
        auditTime(audit, asset.updatedAt());
        var entity = MediaCatalogMapper.toEntity(asset);
        entity.setCreatedAt(asset.createdAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        entity.setUpdatedAt(asset.updatedAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        entityManager.persist(entity);
        entityManager.flush();
        var saved = MediaCatalogMapper.toDomain(entity);
        append(new MediaAuditEvent(null, saved.mediaId(), null, null, MediaAuditOperation.ASSET_REGISTERED,
                audit, null, saved.version()));
        return saved;
    }

    @Override @Transactional
    public MediaAsset updateAssetMetadata(MediaAsset asset, MediaAuditContext audit) {
        require(asset.mediaId() != null, "Asset not registered");
        auditTime(audit, asset.updatedAt());
        var entity = entityManager.find(MediaAssetEntity.class, asset.mediaId());
        require(entity != null, "Asset not found");
        expectedVersion(entity.getVersion(), asset.version());
        var current = MediaCatalogMapper.toDomain(entity);
        require(current.revisePurpose(asset.purpose(), asset.updatedAt()).equals(asset), "Immutable asset attributes changed");
        // No classification/moderation/retention policy is implemented by this foundation.
        require(!current.purpose().equals(asset.purpose()) || !current.updatedAt().equals(asset.updatedAt()), "No metadata change");
        entity.setPurpose(asset.purpose()); entity.setUpdatedAt(asset.updatedAt());
        entityManager.flush();
        append(new MediaAuditEvent(null, asset.mediaId(), null, null, MediaAuditOperation.ASSET_METADATA_UPDATED,
                audit, asset.version(), entity.getVersion()));
        return MediaCatalogMapper.toDomain(entity);
    }

    @Override @Transactional
    public MediaVariant registerVariant(MediaVariant variant, MediaAuditContext audit) {
        require(variant.variantId() == null, "Variant already registered");
        auditTime(audit, variant.createdAt());
        var entity = MediaCatalogMapper.toEntity(variant);
        entityManager.persist(entity); entityManager.flush();
        var saved = MediaCatalogMapper.toDomain(entity);
        append(new MediaAuditEvent(null, saved.mediaId(), saved.variantId(), null,
                MediaAuditOperation.VARIANT_REGISTERED, audit, null, null));
        return saved;
    }

    @Override @Transactional
    public MediaReference recordReference(MediaReference reference, MediaAuditContext audit) {
        require(reference.referenceId() == null, "Reference already recorded");
        entityManager.find(MediaAssetEntity.class,reference.mediaId(),jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        var control=entityManager.find(MediaControlEntity.class,reference.mediaId(),jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        require(control==null || !"DISCARDED".equals(control.getState()),"Media logically discarded");
        if(control!=null && java.util.Set.of(com.jeepclub.backend.storage.core.domain.enums.MediaContext.DEPENDENT_PHOTO,com.jeepclub.backend.storage.core.domain.enums.MediaContext.MEDICAL_IMAGE,com.jeepclub.backend.storage.core.domain.enums.MediaContext.IDENTIFICATION_PHOTO).contains(reference.context())) control.setSensitive(true);
        auditTime(audit, reference.unlinkedAt() == null ? reference.linkedAt() : reference.unlinkedAt());
        var entity = MediaCatalogMapper.toEntity(reference);
        entity.setLinkedAt(reference.linkedAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        if(reference.unlinkedAt()!=null) entity.setUnlinkedAt(reference.unlinkedAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        entityManager.persist(entity); entityManager.flush();
        var saved = MediaCatalogMapper.toDomain(entity);
        append(new MediaAuditEvent(null, saved.mediaId(), null, saved.referenceId(),
                MediaAuditOperation.REFERENCE_RECORDED, audit, null, saved.version()));
        return saved;
    }

    @Override @Transactional
    public MediaReference endReference(MediaReference reference, MediaAuditContext audit) {
        require(reference.referenceId() != null && reference.unlinkedAt() != null, "Missing historical interval");
        entityManager.find(MediaAssetEntity.class,reference.mediaId(),jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        auditTime(audit, reference.unlinkedAt());
        var entity = entityManager.find(MediaReferenceEntity.class, reference.referenceId());
        require(entity != null, "Reference not found");
        expectedVersion(entity.getVersion(), reference.version());
        var current = MediaCatalogMapper.toDomain(entity);
        require(current.end(reference.unlinkedAt(), reference.unlinkedByUserId(), reference.unlinkReasonCode()).equals(reference),
                "Immutable reference attributes changed");
        entity.setUnlinkedAt(reference.unlinkedAt()); entity.setUnlinkedByUserId(reference.unlinkedByUserId());
        entity.setUnlinkReasonCode(reference.unlinkReasonCode());
        entityManager.flush();
        append(new MediaAuditEvent(null, reference.mediaId(), null, reference.referenceId(),
                MediaAuditOperation.REFERENCE_ENDED, audit, reference.version(), entity.getVersion()));
        return MediaCatalogMapper.toDomain(entity);
    }

    private void append(MediaAuditEvent event) {
        entityManager.persist(MediaCatalogMapper.toEntity(event)); entityManager.flush();
    }
    private static void expectedVersion(Long actual, Long expected) {
        if (!Objects.equals(actual, expected)) throw new OptimisticLockException("Concurrent media catalog change");
    }
    private static void auditTime(MediaAuditContext context, Instant changeAt) {
        Objects.requireNonNull(context, "audit");
        require(!context.occurredAt().isBefore(changeAt), "Audit precedes change");
    }
    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}
