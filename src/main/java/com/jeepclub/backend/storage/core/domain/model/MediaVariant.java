package com.jeepclub.backend.storage.core.domain.model;

import com.jeepclub.backend.storage.core.domain.enums.MediaVariantRole;
import java.time.Instant;
import java.util.Objects;

/** Descriptor only: no byte IO, delivery URL, audience or processing implementation. */
public record MediaVariant(Long variantId, Long mediaId, String variantCode, MediaVariantRole role,
        String provider, String namespace, String storageKey, String mimeType, long sizeBytes,
        Integer width, Integer height, String checksumAlgorithm, String checksum,
        String presetCode, String processingVersion, String transformationCode, Instant createdAt) {
    public MediaVariant {
        MediaInvariants.id(variantId, "variantId"); MediaInvariants.requiredId(mediaId, "mediaId");
        Objects.requireNonNull(role); Objects.requireNonNull(createdAt);
        MediaInvariants.token(variantCode, 64, "variantCode");
        MediaInvariants.require(variantCode.equals(variantCode.toUpperCase(java.util.Locale.ROOT)), "variantCode case");
        MediaInvariants.require((role == MediaVariantRole.MASTER) == variantCode.equals("MASTER"), "canonical source");
        MediaInvariants.require(!variantCode.equals("ORIGINAL"), "use MASTER for the canonical source");
        MediaInvariants.token(provider, 32, "provider"); MediaInvariants.token(namespace, 64, "namespace");
        // Opaque provider key. Do not normalize case, paths or legacy location; never a capability.
        MediaInvariants.require(storageKey != null && !storageKey.isBlank() && storageKey.length() <= 512
                && storageKey.chars().noneMatch(Character::isISOControl), "storageKey");
        MediaInvariants.require(mimeType != null && mimeType.length() <= 127
                && mimeType.matches("[a-z0-9][a-z0-9.+-]*/[a-z0-9][a-z0-9.+-]*"), "mimeType");
        MediaInvariants.require(sizeBytes >= 0, "sizeBytes");
        MediaInvariants.require((width == null && height == null) || (width != null && height != null && width > 0 && height > 0), "dimensions");
        MediaInvariants.require((checksumAlgorithm == null) == (checksum == null), "checksum pair");
        MediaInvariants.optionalToken(checksumAlgorithm, 32, "checksumAlgorithm");
        MediaInvariants.require(checksum == null || (checksum.length() <= 128 && checksum.matches("[a-fA-F0-9]+")), "checksum");
        MediaInvariants.optionalToken(presetCode, 64, "presetCode");
        MediaInvariants.optionalToken(processingVersion, 64, "processingVersion");
        MediaInvariants.optionalToken(transformationCode, 64, "transformationCode");
    }
    public boolean isPrivateSource() { return role == MediaVariantRole.MASTER; }
    @Override public String toString() { return "MediaVariant[variantId=" + variantId + ", mediaId=" + mediaId + "]"; }
}
