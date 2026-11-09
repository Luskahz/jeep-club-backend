package com.jeepclub.backend.storage.core.application;

import com.jeepclub.backend.storage.core.domain.enums.*;
import com.jeepclub.backend.storage.core.domain.model.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class ContextualMediaAccessTest {
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final MediaAsset asset = new MediaAsset(1L, MediaOrigin.UPLOAD, MediaContext.DEPENDENT_PHOTO, null, 1L, 1L, MediaSubjectType.DEPENDENT, 1L, "PHOTO", MediaClassification.CLASSIFIED, MediaPhysicalState.PRESENT, now, now, 0L);
    private final MediaReference reference = new MediaReference(1L, 1L, MediaContext.DEPENDENT_PHOTO, "1", "PHOTO", null, MediaOrigin.UPLOAD, now, 1L, null, null, null, 0L);
    private final MediaVariant master = new MediaVariant(1L, 1L, "MASTER", MediaVariantRole.MASTER, "LOCAL", "private-images", "private-images/x", "image/jpeg", 10, 1, 1, null, null, null, null, null, now);
    @Test void moderationAndOwningContextAreRequiredAndMasterIsNeverCacheable() {
        var yes = new ContextualMediaAccess((user, ref, isMaster) -> user == 1 && isMaster);
        assertThat(yes.decide(1L, asset, reference, master, ContextualMediaAccess.Moderation.ALLOWED, ContextualMediaAccess.Moderation.ALLOWED, true).allowed()).isTrue();
        assertThat(yes.decide(1L, asset, reference, master, ContextualMediaAccess.Moderation.ALLOWED, ContextualMediaAccess.Moderation.ALLOWED, true).cache()).isEqualTo(ContextualMediaAccess.Cache.PRIVATE_NO_STORE);
        assertThat(yes.decide(2L, asset, reference, master, ContextualMediaAccess.Moderation.ALLOWED, ContextualMediaAccess.Moderation.ALLOWED, true).allowed()).isFalse();
        assertThat(yes.decide(1L, asset, reference, master, ContextualMediaAccess.Moderation.UNKNOWN, ContextualMediaAccess.Moderation.ALLOWED, true).allowed()).isFalse();
        assertThat(yes.decide(1L, asset, reference.end(now.plusSeconds(1), 1L, "END"), master, ContextualMediaAccess.Moderation.ALLOWED, ContextualMediaAccess.Moderation.ALLOWED, true).allowed()).isTrue();
    }
}
