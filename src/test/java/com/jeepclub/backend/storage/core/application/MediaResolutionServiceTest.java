package com.jeepclub.backend.storage.core.application;

import com.jeepclub.backend.storage.api.module.StorageConsumerPolicy;
import com.jeepclub.backend.storage.core.repository.*;
import com.jeepclub.backend.storage.core.domain.model.*;
import com.jeepclub.backend.storage.core.domain.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class MediaResolutionServiceTest {
    final MediaCatalogRepository catalog=mock(MediaCatalogRepository.class);
    final BlobCatalog blobs=mock(BlobCatalog.class);
    final MediaGovernance governance=mock(MediaGovernance.class);
    final LegacyMediaSource legacy=mock(LegacyMediaSource.class);
    final StorageConsumerPolicy policy=mock(StorageConsumerPolicy.class);
    final MediaResolutionService service=new MediaResolutionService(catalog,blobs,governance,List.of(policy),legacy);
    final byte[] bytes={1,2,3};
    final Instant now=Instant.now();
    final MediaReference ref=new MediaReference(2L,1L,MediaContext.IDENTITY_PROFILE,"11","PHOTO",null,MediaOrigin.UPLOAD,now,11L,null,null,null,0L);

    void fixture(String namespace,String checksum) {
        when(catalog.findAsset(1L)).thenReturn(Optional.of(new MediaAsset(1L,MediaOrigin.UPLOAD,MediaContext.IDENTITY_PROFILE,"11",11L,11L,MediaSubjectType.USER,11L,"PHOTO",MediaClassification.CLASSIFIED,MediaPhysicalState.PRESENT,now,now,0L)));
        when(catalog.findReference(2L)).thenReturn(Optional.of(ref));
        when(catalog.findVariant(1L,"MASTER")).thenReturn(Optional.of(new MediaVariant(3L,1L,"MASTER",MediaVariantRole.MASTER,"LOCAL",namespace,namespace+"/fixture.png","image/png",3,1,1,"SHA-256",checksum,null,null,null,now)));
        when(governance.allows(1L,2L)).thenReturn(true);
        when(policy.context()).thenReturn(MediaContext.IDENTITY_PROFILE);
        when(policy.mayRead(11,ref,true)).thenReturn(true);
    }
    String digest() throws Exception {return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}

    @Test void legacyUsesItsProviderAfterAuthorizationAndVerifiesIntegrity() throws Exception {
        fixture("images",digest());when(legacy.read("images/fixture.png")).thenReturn(bytes);
        assertThat(service.resolve(11,1,2,"MASTER").bytes()).containsExactly(bytes);
        verifyNoInteractions(blobs);
    }
    @Test void deniedAudienceNeverReadsPhysicalContent() throws Exception {
        fixture("images",digest());
        assertThatThrownBy(()->service.resolve(22,1,2,"MASTER")).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(blobs,legacy);
    }
    @Test void moderationPreventsLegacyRead() throws Exception {
        fixture("images",digest());when(governance.allows(1L,2L)).thenReturn(false);
        assertThatThrownBy(()->service.resolve(11,1,2,"MASTER")).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(blobs,legacy);
    }
    @Test void changedLegacyContentIsNotDelivered() throws Exception {
        fixture("images",digest());when(legacy.read("images/fixture.png")).thenReturn(new byte[]{3,2,1});
        assertThatThrownBy(()->service.resolve(11,1,2,"MASTER")).isInstanceOf(MediaIntegrityException.class);
    }
    @Test void missingNewBlobDoesNotFallBackToLegacy() throws Exception {
        fixture("storage-blobs",digest());
        assertThatThrownBy(()->service.resolve(11,1,2,"MASTER")).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(legacy);
    }
    @Test void realLegacyFileIsReadInPlaceAndChangedOrMissingFileIsNeverDelivered(@org.junit.jupiter.api.io.TempDir java.nio.file.Path root) throws Exception {
        fixture("images",digest());
        String key="images/2026/11/12/550e8400-e29b-41d4-a716-446655440000.png";
        when(catalog.findVariant(1L,"MASTER")).thenReturn(Optional.of(new MediaVariant(3L,1L,"MASTER",MediaVariantRole.MASTER,
            "LOCAL","images",key,"image/png",bytes.length,1,1,"SHA-256",digest(),null,null,null,now)));
        var file=root.resolve(key);java.nio.file.Files.createDirectories(file.getParent());java.nio.file.Files.write(file,bytes);
        var modified=java.nio.file.Files.getLastModifiedTime(file);
        var properties=new com.jeepclub.backend.platform.storage.properties.StorageProperties();properties.local().setRootDirectory(root);
        var provider=new com.jeepclub.backend.storage.infra.blob.LegacyMediaSourceAdapter(
            new com.jeepclub.backend.platform.storage.local.LocalFileStorage(properties,java.time.Clock.systemUTC()));
        var resolver=new MediaResolutionService(catalog,blobs,governance,List.of(policy),provider);
        assertThat(resolver.resolve(11,1,2,"MASTER").bytes()).containsExactly(bytes);
        assertThat(java.nio.file.Files.readAllBytes(file)).containsExactly(bytes);
        assertThat(java.nio.file.Files.getLastModifiedTime(file)).isEqualTo(modified);
        verifyNoInteractions(blobs);
        java.nio.file.Files.write(file,new byte[]{3,2,1});
        assertThatThrownBy(()->resolver.resolve(11,1,2,"MASTER")).isInstanceOf(MediaIntegrityException.class);
        java.nio.file.Files.delete(file); // Test fixture only, never application behavior.
        assertThatThrownBy(()->resolver.resolve(11,1,2,"MASTER"))
            .isInstanceOf(com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException.class);
    }
}
