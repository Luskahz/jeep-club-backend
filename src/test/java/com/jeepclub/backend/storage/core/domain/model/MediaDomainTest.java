package com.jeepclub.backend.storage.core.domain.model;

import com.jeepclub.backend.storage.core.domain.enums.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class MediaDomainTest {
    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");
    private static MediaAsset asset(Long uploader, Long owner, MediaSubjectType subject, Long subjectId) {
        return MediaAsset.create(MediaOrigin.UNKNOWN, MediaContext.UNKNOWN, null, uploader, owner, subject, subjectId, "UNKNOWN", NOW);
    }
    private static MediaVariant variant(String code, MediaVariantRole role, Integer width, Integer height,
            String algorithm, String checksum) {
        return new MediaVariant(null, 1L, code, role, "LOCAL", "private-images", "opaque/Case.jpg", "image/jpeg", 5,
                width, height, algorithm, checksum, null, null, null, NOW);
    }
    private static MediaReference reference() {
        return new MediaReference(null, 1L, MediaContext.VEHICLE_PHOTO, "10", "GALLERY", 0,
                MediaOrigin.UNKNOWN, NOW, null, null, null, null, null);
    }
    @Test void unknownProvenanceDoesNotInventUploaderOwnerOrSubject() {
        var value = asset(null, null, MediaSubjectType.UNKNOWN, null);
        assertThat(value.uploadedByUserId()).isNull(); assertThat(value.ownerUserId()).isNull();
        assertThat(value.subjectId()).isNull(); assertThat(value.physicalState()).isEqualTo(MediaPhysicalState.UNKNOWN);
        assertThat(value.classification()).isEqualTo(MediaClassification.UNCLASSIFIED);
    }
    @Test void uploaderOwnerAndSubjectAreIndependent() {
        var value = asset(11L, 22L, MediaSubjectType.DEPENDENT, 33L);
        assertThat(value.uploadedByUserId()).isEqualTo(11); assertThat(value.ownerUserId()).isEqualTo(22);
        assertThat(value.subjectId()).isEqualTo(33);
        assertThatThrownBy(() -> asset(null, null, MediaSubjectType.UNKNOWN, 33L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> asset(null, null, MediaSubjectType.USER, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> asset(0L, null, MediaSubjectType.UNKNOWN, null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void masterIsPrivateCanonicalAndNeedNotMatchUploadBytes() {
        var master = variant("MASTER", MediaVariantRole.MASTER, 20, 30, "SHA-256", "abcdef");
        assertThat(master.isPrivateSource()).isTrue();
        assertThat(master.storageKey()).isEqualTo("opaque/Case.jpg");
        assertThat(master.presetCode()).isNull();
        assertThatThrownBy(() -> variant("MASTER", MediaVariantRole.DERIVED, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> variant("ORIGINAL", MediaVariantRole.DERIVED, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> variant("FEED", MediaVariantRole.MASTER, null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void dimensionsAndChecksumsMustBeCoherentWhileUnknownIsAllowed() {
        assertThat(variant("MASTER", MediaVariantRole.MASTER, null, null, null, null).width()).isNull();
        assertThatThrownBy(() -> variant("MASTER", MediaVariantRole.MASTER, 1, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> variant("MASTER", MediaVariantRole.MASTER, 0, 1, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> variant("MASTER", MediaVariantRole.MASTER, 1, 1, "SHA-256", null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void referenceEndsOnceWithoutReactivatingOrLosingItsConsumer() {
        var current = reference(); var historical = current.end(NOW.plusSeconds(1), null, null);
        assertThat(current.isCurrent()).isTrue(); assertThat(historical.isCurrent()).isFalse();
        assertThat(historical.consumerId()).isEqualTo(current.consumerId());
        assertThat(historical.linkedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> historical.end(NOW.plusSeconds(2), null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> current.end(NOW.minusSeconds(1), null, null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void timestampsAndMetadataCannotGoBackwards() {
        var value = asset(null, null, MediaSubjectType.UNKNOWN, null);
        assertThatThrownBy(() -> value.revisePurpose("UNKNOWN", NOW.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> value.revisePurpose("private note with spaces", NOW)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void auditSeparatesSystemUnknownAndKnownUserAndContainsNoFreeText() {
        assertThat(new MediaAuditContext(MediaActorKind.UNKNOWN, null, NOW, null).actorUserId()).isNull();
        assertThatThrownBy(() -> new MediaAuditContext(MediaActorKind.USER, null, NOW, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MediaAuditContext(MediaActorKind.SYSTEM, 1L, NOW, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MediaAuditContext(MediaActorKind.USER, 1L, NOW, "paths/are/not/attributes")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void objectSummariesDoNotPrintTechnicalKeysOrPersonalAttributes() {
        assertThat(variant("MASTER", MediaVariantRole.MASTER, null, null, null, null).toString()).doesNotContain("opaque", "Case", "private-images", "jpeg");
        assertThat(asset(11L, 22L, MediaSubjectType.USER, 33L).toString()).doesNotContain("11", "22", "33");
        assertThat(reference().toString()).doesNotContain("GALLERY", "VEHICLE");
    }
}
