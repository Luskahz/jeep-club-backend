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
        assertThatThrownBy(()->service.resolve(11,1,2,"MASTER")).isInstanceOf(AccessDeniedException.class);
    }
    @Test void missingNewBlobDoesNotFallBackToLegacy() throws Exception {
        fixture("storage-blobs",digest());
        assertThatThrownBy(()->service.resolve(11,1,2,"MASTER")).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(legacy);
    }
}
