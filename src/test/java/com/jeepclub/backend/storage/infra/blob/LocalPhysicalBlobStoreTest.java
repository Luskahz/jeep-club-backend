package com.jeepclub.backend.storage.infra.blob;

import com.jeepclub.backend.platform.storage.properties.StorageProperties;
import com.jeepclub.backend.storage.infra.persistence.adapter.BlobCatalogAdapter;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import static org.assertj.core.api.Assertions.*;

class LocalPhysicalBlobStoreTest {
    @Test void privateObjectsUseRestrictivePosixModesWhenSupported() throws Exception {
        var root=Files.createTempDirectory("storage-mode-fixture-");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.getFileStore(root).supportsFileAttributeView(java.nio.file.attribute.PosixFileAttributeView.class));
        var properties=new StorageProperties();properties.local().setRootDirectory(root);var physical=new LocalPhysicalBlobStore(properties);
        byte[] bytes={4,5,6};String hash=BlobCatalogAdapter.digest(bytes);physical.putVerified(hash,bytes);
        assertThat(Files.getPosixFilePermissions(root.resolve("storage-blobs"))).isEqualTo(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        assertThat(Files.getPosixFilePermissions(root.resolve("storage-blobs").resolve(hash+".bin"))).isEqualTo(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
    }
    @Test void inventoryResumesWithoutRestartingTheDirectoryAndBoundsEachPage() throws Exception {
        var root=Files.createTempDirectory("storage-page-fixture-");var properties=new StorageProperties();properties.local().setRootDirectory(root);
        var physical=new LocalPhysicalBlobStore(properties);
        for(int i=1;i<=5;i++){byte[] bytes={(byte)i};physical.putVerified(BlobCatalogAdapter.digest(bytes),bytes);}
        var names=new java.util.HashSet<String>();String cursor=null;int pages=0;
        do {var page=physical.inventory(cursor,2);assertThat(page.filenames()).hasSizeLessThanOrEqualTo(2);names.addAll(page.filenames());cursor=page.nextCursor();pages++;}while(cursor!=null);
        assertThat(names).hasSize(5);assertThat(pages).isEqualTo(3);
        assertThatThrownBy(() -> physical.inventory("expired",2)).isInstanceOf(IllegalArgumentException.class);
        physical.closeInventories();
    }
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

