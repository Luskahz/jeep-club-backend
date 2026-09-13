package com.jeepclub.backend.shared.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StorageContractsTest {

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
