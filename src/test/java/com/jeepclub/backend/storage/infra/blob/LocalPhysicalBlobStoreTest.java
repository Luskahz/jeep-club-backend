package com.jeepclub.backend.storage.infra.blob;

import com.jeepclub.backend.platform.storage.properties.StorageProperties;
import com.jeepclub.backend.storage.infra.persistence.adapter.BlobCatalogAdapter;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import static org.assertj.core.api.Assertions.*;

class LocalPhysicalBlobStoreTest {
    @Test void corruptionIsRejectedAndExistingCustodyIsNeverOverwritten() throws Exception {
        var root=Files.createTempDirectory("storage-corrupt-fixture-");var properties=new StorageProperties();properties.local().setRootDirectory(root);
        var physical=new LocalPhysicalBlobStore(properties);byte[] bytes={1,2,3};String hash=BlobCatalogAdapter.digest(bytes);
        physical.putVerified(hash,bytes);var target=root.resolve("storage-blobs").resolve(hash+".bin");
        Files.write(target,new byte[]{3,2,1});
        assertThatThrownBy(() -> physical.readVerified(hash,3)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> physical.putVerified(hash,bytes)).isInstanceOf(IllegalStateException.class);
        assertThat(Files.readAllBytes(target)).containsExactly(new byte[]{3,2,1});
    }
}

