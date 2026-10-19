package com.jeepclub.backend.platform.storage.image;

import com.jeepclub.backend.shared.storage.*;
import com.jeepclub.backend.shared.storage.exception.InvalidStorageKeyException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PrivateImageMediaContractTest {
    static final String PRIVATE="private-images/2026/10/19/550e8400-e29b-41d4-a716-446655440000.png";
    static final String PUBLIC="images/2026/10/19/550e8400-e29b-41d4-a716-446655440000.png";
    @AfterEach void resetTransaction() {
        if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }
    @Test void namespacesAreDisjointAndNoPublicResolverCanReadAPrivateReference() {
        assertThat(PrivateImageReference.require(PRIVATE)).isEqualTo(PRIVATE);
        assertThatThrownBy(() -> ImageReference.require(PRIVATE)).isInstanceOf(InvalidStorageKeyException.class);
        assertThatThrownBy(() -> PrivateImageReference.require(PUBLIC)).isInstanceOf(InvalidStorageKeyException.class);
        var storage=mock(FileStorage.class);
        assertThatThrownBy(() -> new ImageMediaService(storage).load(PRIVATE)).isInstanceOf(InvalidStorageKeyException.class);
        verifyNoInteractions(storage);
    }
    @Test void privateCommandUsesTheSameCompensationOnlyForItsFreshObject() {
        var images=mock(ImageMediaService.class);var storage=mock(FileStorage.class);
        when(images.storePrivate(any(),any(),any())).thenReturn(PRIVATE);
        TransactionSynchronizationManager.setActualTransactionActive(true);TransactionSynchronizationManager.initSynchronization();
        var value=new TransactionalImageMediaCommand(images,storage).storePrivateInTransaction(new ImageUpload("photo.png","image/png",new byte[]{1}));
        assertThat(value).isEqualTo(PRIVATE);verify(images,never()).store(any(),any(),any());
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(storage).delete(PRIVATE);verifyNoMoreInteractions(storage);
    }
    @Test void privateCommandRejectsNoTransactionBeforeWriting() {
        var images=mock(ImageMediaService.class);var storage=mock(FileStorage.class);
        assertThatThrownBy(() -> new TransactionalImageMediaCommand(images,storage).storePrivateInTransaction(new ImageUpload("photo.png","image/png",new byte[]{1})))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(images,storage);
    }
    @Test void privateReadResultContainsNoReferenceAndCopiesItsBytes() {
        var storage=mock(FileStorage.class);byte[] bytes={1,2};when(storage.load(PRIVATE)).thenReturn(new StorageResource(PRIVATE,bytes));
        var value=new ImageMediaService(storage).loadPrivate(PRIVATE);bytes[0]=9;value.content()[1]=9;
        assertThat(value.contentType()).isEqualTo("image/png");assertThat(value.content()).containsExactly(1,2);assertThat(value.size()).isEqualTo(2);
        assertThat(value.toString()).doesNotContain(PRIVATE);
    }
}
