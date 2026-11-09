package com.jeepclub.backend.storage.core.repository;

import java.util.Optional;

/** Internal custody API. Digests and physical keys must never be public DTOs. No deletion capability. */
public interface BlobCatalog {
    record Blob(String sha256, long sizeBytes, String storageKey) {}
    Blob store(byte[] bytes);
    void bind(Long variantId, Blob blob);
    Optional<Blob> findByVariant(Long variantId);
    byte[] read(Blob blob);
}
