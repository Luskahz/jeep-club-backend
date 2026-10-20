package com.jeepclub.backend.platform.storage.image;

import com.jeepclub.backend.shared.storage.FileStorage;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageFileException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionalImageMediaCommandTest {
    static final String KEY = "images/2026/10/18/550e8400-e29b-41d4-a716-446655440000.png";
    @Mock ImageMediaService images;
    @Mock FileStorage storage;
    ImageUpload upload() { return new ImageUpload("photo.png", "image/png", new byte[]{1}); }
    void begin() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }
    void complete(int status) {
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(status));
    }
    @AfterEach void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }
    String store() {
        begin(); when(images.store(anyString(), anyString(), any())).thenReturn(KEY);
        return new TransactionalImageMediaCommand(images, storage).storeInTransaction(upload());
    }
    @Test void noActiveTransactionDoesNotCreateAnyObject() {
        assertThatThrownBy(() -> new TransactionalImageMediaCommand(images, storage).storeInTransaction(upload()))
            .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(images, storage);
    }
    @Test void commitKeepsFreshImage() {
        assertThat(store()).isEqualTo(KEY); complete(TransactionSynchronization.STATUS_COMMITTED);
        verifyNoInteractions(storage);
    }
    @Test void rollbackDeletesOnlyFreshImage() {
        store(); complete(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(storage).delete(KEY); verifyNoMoreInteractions(storage);
    }
    @Test void unknownCommitOutcomeRetainsImageForReconciliation() {
        store(); complete(TransactionSynchronization.STATUS_UNKNOWN); verifyNoInteractions(storage);
    }
    @Test void cleanupFailureIsSanitizedAndCannotReplaceOriginalRollbackError() {
        store(); doThrow(new IllegalStateException("secret-path-and-key=" + KEY)).when(storage).delete(KEY);
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(TransactionalImageMediaCommand.class);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start(); logger.addAppender(appender);
        try {
            assertThatCode(() -> complete(TransactionSynchronization.STATUS_ROLLED_BACK)).doesNotThrowAnyException();
            assertThat(appender.list).singleElement().satisfies(e -> {
                assertThat(e.getFormattedMessage()).doesNotContain(KEY, "secret-path-and-key");
                assertThat(e.getThrowableProxy()).isNull();
            });
        } finally { logger.detachAppender(appender); appender.stop(); }
    }
    @Test void uploadFailureRegistersNoCleanupOrPersistence() {
        begin(); var original = new IllegalStateException("Synthetic unavailable storage");
        when(images.store(anyString(), anyString(), any())).thenThrow(original);
        assertThatThrownBy(() -> new TransactionalImageMediaCommand(images, storage).storeInTransaction(upload())).isSameAs(original);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty(); verifyNoInteractions(storage);
    }
    @Test void invalidSignatureIsRejectedBeforeStorage() {
        begin(); var command = new TransactionalImageMediaCommand(new ImageMediaService(storage), storage);
        assertThatThrownBy(() -> command.storeInTransaction(upload())).isInstanceOf(InvalidStorageFileException.class);
        verifyNoInteractions(storage);
    }
    @Test void uploadBytesAreDefensivelyCopied() {
        byte[] source = {1, 2}; var image = new ImageUpload("photo.png", "image/png", source);
        source[0] = 9; image.content()[1] = 9; assertThat(image.content()).containsExactly(1, 2);
    }
}
