package com.jeepclub.backend.storage.infra.persistence.mapper;

import com.jeepclub.backend.storage.core.domain.model.*;
import com.jeepclub.backend.storage.infra.persistence.entity.*;

public final class MediaCatalogMapper {
    public static MediaAssetEntity toEntity(MediaAsset value) {
        var entity = new MediaAssetEntity();
        entity.setMediaId(value.mediaId());
        entity.setOrigin(value.origin());
        entity.setSourceContext(value.sourceContext());
        entity.setSourceRecordId(value.sourceRecordId());
        entity.setUploadedByUserId(value.uploadedByUserId());
        entity.setOwnerUserId(value.ownerUserId());
        entity.setSubjectType(value.subjectType());
        entity.setSubjectId(value.subjectId());
        entity.setPurpose(value.purpose());
        entity.setClassification(value.classification());
        entity.setPhysicalState(value.physicalState());
        entity.setCreatedAt(value.createdAt());
        entity.setUpdatedAt(value.updatedAt());
        entity.setVersion(value.version());
        return entity;
    }
    public static MediaAsset toDomain(MediaAssetEntity entity) {
        return new MediaAsset(
                entity.getMediaId(),
                entity.getOrigin(),
                entity.getSourceContext(),
                entity.getSourceRecordId(),
                entity.getUploadedByUserId(),
                entity.getOwnerUserId(),
                entity.getSubjectType(),
                entity.getSubjectId(),
                entity.getPurpose(),
                entity.getClassification(),
                entity.getPhysicalState(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion());
    }

    public static MediaVariantEntity toEntity(MediaVariant value) {
        var entity = new MediaVariantEntity();
        entity.setVariantId(value.variantId());
        entity.setMediaId(value.mediaId());
        entity.setVariantCode(value.variantCode());
        entity.setRole(value.role());
        entity.setProvider(value.provider());
        entity.setNamespace(value.namespace());
        entity.setStorageKey(value.storageKey());
        entity.setMimeType(value.mimeType());
        entity.setSizeBytes(value.sizeBytes());
        entity.setWidth(value.width());
        entity.setHeight(value.height());
        entity.setChecksumAlgorithm(value.checksumAlgorithm());
        entity.setChecksum(value.checksum());
        entity.setPresetCode(value.presetCode());
        entity.setProcessingVersion(value.processingVersion());
        entity.setTransformationCode(value.transformationCode());
        entity.setCreatedAt(value.createdAt());
        return entity;
    }
    public static MediaVariant toDomain(MediaVariantEntity entity) {
        return new MediaVariant(
                entity.getVariantId(),
                entity.getMediaId(),
                entity.getVariantCode(),
                entity.getRole(),
                entity.getProvider(),
                entity.getNamespace(),
                entity.getStorageKey(),
                entity.getMimeType(),
                entity.getSizeBytes(),
                entity.getWidth(),
                entity.getHeight(),
                entity.getChecksumAlgorithm(),
                entity.getChecksum(),
                entity.getPresetCode(),
                entity.getProcessingVersion(),
                entity.getTransformationCode(),
                entity.getCreatedAt());
    }

    public static MediaReferenceEntity toEntity(MediaReference value) {
        var entity = new MediaReferenceEntity();
        entity.setReferenceId(value.referenceId());
        entity.setMediaId(value.mediaId());
        entity.setContext(value.context());
        entity.setConsumerId(value.consumerId());
        entity.setRole(value.role());
        entity.setPosition(value.position());
        entity.setAssociationOrigin(value.associationOrigin());
        entity.setLinkedAt(value.linkedAt());
        entity.setLinkedByUserId(value.linkedByUserId());
        entity.setUnlinkedAt(value.unlinkedAt());
        entity.setUnlinkedByUserId(value.unlinkedByUserId());
        entity.setUnlinkReasonCode(value.unlinkReasonCode());
        entity.setVersion(value.version());
        return entity;
    }
    public static MediaReference toDomain(MediaReferenceEntity entity) {
        return new MediaReference(
                entity.getReferenceId(),
                entity.getMediaId(),
                entity.getContext(),
                entity.getConsumerId(),
                entity.getRole(),
                entity.getPosition(),
                entity.getAssociationOrigin(),
                entity.getLinkedAt(),
                entity.getLinkedByUserId(),
                entity.getUnlinkedAt(),
                entity.getUnlinkedByUserId(),
                entity.getUnlinkReasonCode(),
                entity.getVersion());
    }

    public static MediaAuditEventEntity toEntity(MediaAuditEvent value) {
        var entity = new MediaAuditEventEntity();
        entity.setEventId(value.eventId());
        entity.setMediaId(value.mediaId());
        entity.setVariantId(value.variantId());
        entity.setReferenceId(value.referenceId());
        entity.setOperation(value.operation());
        entity.setActorKind(value.actor().actorKind());
        entity.setActorUserId(value.actor().actorUserId());
        entity.setOccurredAt(value.actor().occurredAt());
        entity.setCorrelationId(value.actor().correlationId());
        entity.setPreviousVersion(value.previousVersion());
        entity.setResultingVersion(value.resultingVersion());
        return entity;
    }
    public static MediaAuditEvent toDomain(MediaAuditEventEntity entity) {
        return new MediaAuditEvent(
                entity.getEventId(),
                entity.getMediaId(),
                entity.getVariantId(),
                entity.getReferenceId(),
                entity.getOperation(),
                new MediaAuditContext(entity.getActorKind(), entity.getActorUserId(), entity.getOccurredAt(), entity.getCorrelationId()),
                entity.getPreviousVersion(),
                entity.getResultingVersion());
    }

    private MediaCatalogMapper() {}
}
