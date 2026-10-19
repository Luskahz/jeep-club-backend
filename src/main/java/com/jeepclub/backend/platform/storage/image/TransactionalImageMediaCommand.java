package com.jeepclub.backend.platform.storage.image;

import com.jeepclub.backend.shared.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionalImageMediaCommand implements ImageMediaCommand {
    private final ImageMediaService images;
    private final FileStorage storage;

    @Override public String storeInTransaction(ImageUpload image) {
        return store(image, false);
    }

    @Override public String storePrivateInTransaction(ImageUpload image) {
        return store(image, true);
    }

    private String store(ImageUpload image, boolean privateImage) {
        Objects.requireNonNull(image, "image cannot be null");
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Image upload requires an active database transaction.");
        }
        String freshKey = privateImage ? images.storePrivate(image.filename(), image.contentType(), image.content())
                : images.store(image.filename(), image.contentType(), image.content());
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) compensate(freshKey);
                    else if (status == STATUS_UNKNOWN) {
                        // Unknown commit outcome may have persisted a reference: retain rather than break it.
                        log.warn("Image transaction outcome unknown; reconciliation required");
                    }
                }
            });
        } catch (RuntimeException original) {
            compensate(freshKey);
            throw original;
        }
        return freshKey;
    }

    private void compensate(String freshKey) {
        try {
            storage.delete(freshKey);
        } catch (RuntimeException failure) {
            // Do not log keys, file names, provider messages, paths or exception stack traces.
            log.warn("Image rollback compensation failed; category={}", failure.getClass().getSimpleName());
        }
    }
}
