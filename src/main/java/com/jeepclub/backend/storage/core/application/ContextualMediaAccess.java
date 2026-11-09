package com.jeepclub.backend.storage.core.application;

import com.jeepclub.backend.storage.core.domain.enums.MediaPhysicalState;
import com.jeepclub.backend.storage.core.domain.enums.MediaContext;
import com.jeepclub.backend.storage.core.domain.model.MediaAsset;
import com.jeepclub.backend.storage.core.domain.model.MediaReference;
import com.jeepclub.backend.storage.core.domain.model.MediaVariant;
import java.util.Objects;

/** Fail-closed decision boundary; the owning context supplies the authorization decision. */
public final class ContextualMediaAccess {
    public interface ConsumerAuthorization {
        boolean mayRead(long userId, MediaReference reference, boolean privateMaster);
    }
    public enum Moderation { ALLOWED, DENIED, UNKNOWN }
    public enum Cache { PRIVATE_NO_STORE, PRIVATE_REVALIDATE }
    public record Decision(boolean allowed, Cache cache) {}
    private final ConsumerAuthorization authorization;
    public ContextualMediaAccess(ConsumerAuthorization authorization) { this.authorization = Objects.requireNonNull(authorization); }
    public Decision decide(Long userId, MediaAsset asset, MediaReference reference, MediaVariant variant,
                           Moderation global, Moderation contextual, boolean protectedContext) {
        if (userId == null || userId <= 0 || asset == null || reference == null || variant == null
                || asset.mediaId() == null || !asset.mediaId().equals(reference.mediaId()) || !asset.mediaId().equals(variant.mediaId())
                || reference.referenceId() == null || variant.variantId() == null || asset.physicalState() != MediaPhysicalState.PRESENT
                || global != Moderation.ALLOWED || contextual != Moderation.ALLOWED)
            return new Decision(false, Cache.PRIVATE_NO_STORE);
        boolean master = variant.isPrivateSource();
        if (!authorization.mayRead(userId, reference, master)) return new Decision(false, Cache.PRIVATE_NO_STORE);
        return new Decision(true, master || protectedContext || java.util.Set.of(MediaContext.DEPENDENT_PHOTO,MediaContext.MEDICAL_IMAGE,MediaContext.IDENTIFICATION_PHOTO).contains(reference.context()) ? Cache.PRIVATE_NO_STORE : Cache.PRIVATE_REVALIDATE);
    }
}
