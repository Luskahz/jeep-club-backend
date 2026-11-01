package com.jeepclub.backend.shared.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StorageContractsTest {
    @Test void absentBinaryContentIsExplicitlyRepresentedAsZeroSizeWithoutInventingBytes() {
        var resource = new StorageResource("synthetic/key",null);
        assertThat(resource.size()).isZero();
        assertThat(resource.content()).isNull();
        assertThat(new StorageResource("synthetic/empty",new byte[0]).content()).isEmpty();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"https://host/image.png","../image.png","private-images/2026/09/24/550e8400-e29b-41d4-a716-446655440000.png","images/../../x.png","images/not-a-key.gif",""})
    @org.junit.jupiter.params.provider.NullSource
    void imageReferenceRejectsUrlsPrivateNamespaceAndTraversal(String key) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ImageReference.require(key))
                .isInstanceOf(com.jeepclub.backend.shared.storage.exception.InvalidStorageKeyException.class);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"png","jpg","webp"})
    void imageReferencesRemainProviderNeutral(String extension) {
        String key = "images/2026/09/24/550e8400-e29b-41d4-a716-446655440000." + extension;
        assertThat(ImageReference.require(key)).isEqualTo(key);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(com.jeepclub.backend.shared.storage.exception.StorageOperationException.Operation.class)
    void storageFailureKeepsOperationAndCauseButItsMessageDoesNotExposePhysicalPath(
            com.jeepclub.backend.shared.storage.exception.StorageOperationException.Operation operation) {
        var cause = new java.io.IOException("synthetic-private-root/file");
        var error = new com.jeepclub.backend.shared.storage.exception.StorageOperationException(operation,cause);
        assertThat(error.operation()).isEqualTo(operation);
        assertThat(error).hasCause(cause).hasMessage(switch(operation) {
            case READ -> "Could not read stored file.";
            case WRITE -> "Could not store file.";
            case DELETE -> "Could not delete stored file.";
        });
        assertThat(error.getMessage()).doesNotContain("synthetic-private-root");
    }

    @Test
    void storageFileShouldDefensivelyCopyBinaryContent() {
        byte[] source = {1, 2, 3};
        StorageFile file = new StorageFile("name.bin", "application/octet-stream", "bin", source);

        source[0] = 9;
        byte[] returned = file.content();
        returned[1] = 9;

        assertThat(file.content()).containsExactly(1, 2, 3);
    }

    @Test
    void storageResourceShouldDefensivelyCopyContentAndExposeDerivedSize() {
        byte[] source = {4, 5};
        StorageResource resource = new StorageResource("namespace/file.bin", source);

        source[0] = 9;
        byte[] returned = resource.content();
        returned[1] = 9;

        assertThat(resource.content()).containsExactly(4, 5);
        assertThat(resource.size()).isEqualTo(2);
    }
}
