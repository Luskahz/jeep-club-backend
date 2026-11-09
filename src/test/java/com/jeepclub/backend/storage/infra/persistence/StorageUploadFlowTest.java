package com.jeepclub.backend.storage.infra.persistence;

import com.jeepclub.backend.storage.api.module.StorageConsumerPolicy;
import com.jeepclub.backend.storage.core.application.*;
import com.jeepclub.backend.storage.core.domain.enums.*;
import com.jeepclub.backend.storage.core.domain.model.*;
import com.jeepclub.backend.storage.core.repository.*;
import com.jeepclub.backend.storage.infra.persistence.adapter.*;
import com.jeepclub.backend.storage.infra.persistence.entity.MediaAssetEntity;
import com.jeepclub.backend.storage.infra.blob.LocalPhysicalBlobStore;
import com.jeepclub.backend.storage.infra.image.MediaImageProcessingAdapter;
import com.jeepclub.backend.platform.storage.properties.StorageProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;

@org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase(replace=org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest(showSql=false,properties="storage.legacy-backfill.enabled=true") @ActiveProfiles("test")
@ContextConfiguration(classes=StorageUploadFlowTest.Configuration.class)
@Import({MediaCatalogRepositoryAdapter.class,BlobCatalogAdapter.class,LocalPhysicalBlobStore.class,MediaGovernanceAdapter.class,
    MediaImageProcessingAdapter.class,MediaUploadService.class,MediaResolutionService.class,MediaReferenceService.class,MediaAdministrationAdapter.class,BlobInspectionAdapter.class,LegacyBackfillAdapter.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class StorageUploadFlowTest {
    @TestConfiguration @EntityScan(basePackageClasses=MediaAssetEntity.class)
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages="com.jeepclub.backend.storage.infra.persistence")
    static class Configuration {
        @Bean StorageProperties properties() throws Exception {
            var p=new StorageProperties();p.local().setRootDirectory(Files.createTempDirectory("storage-wave3-test-"));return p;
        }
        @Bean LegacyFixture legacySource() { return new LegacyFixture(); }
        @Bean SimpleMeterRegistry metrics() { return new SimpleMeterRegistry(); }
        @Bean StorageConsumerPolicy policy() { return new StorageConsumerPolicy() {
            public MediaContext context() { return MediaContext.IDENTITY_PROFILE; }
            public boolean mayUpload(long actor,String id) { return Long.toString(actor).equals(id); }
            public boolean mayRead(long actor,MediaReference ref,boolean master) { return mayUpload(actor,ref.consumerId()); }
        }; }
    }
    static class LegacyFixture implements LegacyMediaSource {
        final java.util.concurrent.ConcurrentMap<String,byte[]> changes=new ConcurrentHashMap<>();
        public byte[] read(String key) {return changes.getOrDefault(key,("legacy-fixture:"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8)).clone();}
    }
    @Autowired LegacyFixture legacySource;
    @Autowired MediaUploadService upload; @Autowired MediaResolutionService resolution;
    @Autowired BlobCatalog blobs; @Autowired PhysicalBlobStore physical; @Autowired MediaCatalogRepository catalog;
    @Autowired MediaGovernance governance; @Autowired JdbcTemplate jdbc; @Autowired PlatformTransactionManager manager;
    @Autowired MediaAdministration admin; @Autowired MediaReferenceService references; @Autowired LegacyBackfill legacy; @Autowired BlobInspection inspection;
    byte[] image(int color) throws Exception {
        var image=new BufferedImage(80,60,BufferedImage.TYPE_INT_RGB);image.setRGB(1,1,color);
        var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();
    }
    @Test void identicalUploadsHaveIndependentIdentityAndSharePhysicalRepresentations() throws Exception {
        var a=upload.upload(11,MediaContext.IDENTITY_PROFILE,"11",image(1));
        var b=upload.upload(22,MediaContext.IDENTITY_PROFILE,"22",image(1));
        assertThat(a.mediaId()).isNotEqualTo(b.mediaId());
        var va=catalog.findVariant(a.mediaId(),"MASTER").orElseThrow();var vb=catalog.findVariant(b.mediaId(),"MASTER").orElseThrow();
        assertThat(blobs.findByVariant(va.variantId())).isEqualTo(blobs.findByVariant(vb.variantId()));
        assertThat(resolution.resolve(11,a.mediaId(),a.referenceId(),"MASTER").bytes()).isNotEmpty();
        assertThatThrownBy(() -> resolution.resolve(22,a.mediaId(),a.referenceId(),"MASTER")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> resolution.resolve(11,a.mediaId(),b.referenceId(),"MASTER")).isInstanceOf(AccessDeniedException.class);
        governance.moderate(new ModerationDecision(a.mediaId(),null,ModerationDecision.State.BLOCKED,ModerationDecision.Reason.PRIVACY,"private note",11,Instant.now()));
        assertThatThrownBy(() -> resolution.resolve(11,a.mediaId(),a.referenceId(),"MASTER")).isInstanceOf(AccessDeniedException.class);
        assertThat(resolution.resolve(22,b.mediaId(),b.referenceId(),"MASTER").bytes()).isNotEmpty();
    }
    @Test void concurrentStoresCommitOneBlobAndNoPartialObject() throws Exception {
        byte[] bytes=image(2);var pool=Executors.newFixedThreadPool(4);var start=new CountDownLatch(1);
        try {
            var tasks=new ArrayList<Future<BlobCatalog.Blob>>();
            for(int i=0;i<4;i++) tasks.add(pool.submit(() -> {start.await();return blobs.store(bytes);}));
            start.countDown();var first=tasks.get(0).get(20,TimeUnit.SECONDS);
            for(var task:tasks) assertThat(task.get(20,TimeUnit.SECONDS)).isEqualTo(first);
            assertThat(jdbc.queryForObject("select count(*) from storage_blobs where sha256=?",Long.class,first.sha256())).isEqualTo(1);
            assertThat(blobs.read(first)).containsExactly(bytes);
        } finally { pool.shutdownNow(); }
    }
    @Test void logicalRollbackPreservesKnownBlobCustody() throws Exception {
        var blob=blobs.store(image(3));
        var tx=new TransactionTemplate(manager);
        assertThatThrownBy(() -> tx.execute(status -> {
            var now=Instant.now();catalog.registerAsset(MediaAsset.create(MediaOrigin.UPLOAD,MediaContext.IDENTITY_PROFILE,"33",33L,33L,MediaSubjectType.USER,33L,"PHOTO",now),new MediaAuditContext(MediaActorKind.USER,33L,now,null));
            throw new IllegalStateException("simulate logical failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("select count(*) from storage_media_assets where owner_user_id=33",Long.class)).isZero();
        assertThat(blobs.read(blob)).isNotEmpty();
        assertThat(jdbc.queryForObject("select count(*) from storage_blobs where sha256=?",Long.class,blob.sha256())).isEqualTo(1);
    }
    @Test void failedUploadRollsBackAssetVariantsReferencesAndAuditButRetainsBlob() throws Exception {
        byte[] bytes=image(77);
        var image=new MediaImageProcessor.Representation("MASTER",bytes,"image/png",80,60);
        MediaImageProcessor broken=(input,context) -> new MediaImageProcessor.Processed(BlobCatalogAdapter.digest(input),List.of(image,image));
        var service=new MediaUploadService(catalog,blobs,broken,governance,List.of(new Configuration().policy()),manager,new SimpleMeterRegistry());
        assertThatThrownBy(() -> service.upload(77,MediaContext.IDENTITY_PROFILE,"77",bytes)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("select count(*) from storage_media_assets where owner_user_id=77",Long.class)).isZero();
        var hash=BlobCatalogAdapter.digest(bytes);
        assertThat(jdbc.queryForObject("select count(*) from storage_blobs where sha256=?",Long.class,hash)).isEqualTo(1);
        assertThat(physical.readVerified(hash,bytes.length)).containsExactly(bytes);
    }
    @Test void retentionHoldDiscardAndSensitiveHistoryArePersisted() throws Exception {
        var a=upload.upload(55,MediaContext.IDENTITY_PROFILE,"55",image(5));
        Instant now=Instant.now();var audit=new MediaAuditContext(MediaActorKind.USER,55L,now,null);
        assertThatThrownBy(() -> admin.discard(a.mediaId(),audit)).isInstanceOf(IllegalArgumentException.class);
        var sensitive=catalog.recordReference(new MediaReference(null,a.mediaId(),MediaContext.IDENTIFICATION_PHOTO,"55","CARD",null,MediaOrigin.UPLOAD,now,55L,null,null,null,null),audit);
        references.dissociate(55,a.mediaId(),a.referenceId());
        now=Instant.now();catalog.endReference(sensitive.end(now,55L,"END"),new MediaAuditContext(MediaActorKind.USER,55L,now,null));
        assertThat(admin.review(a.mediaId(),new MediaAuditContext(MediaActorKind.USER,55L,now.plusSeconds(91L*86400),null)).retentionClass()).isEqualTo("SENSITIVE");
        governance.legalHold(a.mediaId(),true,"Retention fixture obligation",audit);
        assertThat(admin.review(a.mediaId(),audit).decision()).isEqualTo("LEGAL_HOLD");
        admin.discard(a.mediaId(),audit);
        assertThatThrownBy(() -> resolution.resolve(55,a.mediaId(),a.referenceId(),"MASTER")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> references.associate(55,a.mediaId(),MediaContext.IDENTITY_PROFILE,"55","PHOTO")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void backfillIsIdempotentUnknownHeldAndRollbackPreservesBytes() {
        var audit=new MediaAuditContext(MediaActorKind.SYSTEM,null,Instant.now(),null);
        long before=jdbc.queryForObject("select count(*) from storage_blobs",Long.class);
        var first=legacy.apply("images/fixture.jpg",audit);var second=legacy.apply("images/fixture.jpg",audit);
        assertThat(jdbc.queryForObject("select count(*) from storage_blobs",Long.class)).isEqualTo(before);
        assertThat(first.mediaId()).isEqualTo(second.mediaId());
        assertThat(catalog.findAsset(first.mediaId()).orElseThrow().ownerUserId()).isNull();
        assertThat(admin.review(first.mediaId(),audit).decision()).isEqualTo("LEGAL_HOLD");
        governance.legalHold(first.mediaId(),false,"Authorized fixture release",audit);
        assertThat(admin.review(first.mediaId(),audit).decision()).isEqualTo("INDETERMINATE");
        governance.legalHold(first.mediaId(),true,"Retention fixture obligation",audit);
        legacy.rollback("images/fixture.jpg",audit);
        assertThat(legacy.inspect("images/fixture.jpg").state()).isEqualTo("ROLLED_BACK");
        var variant=catalog.findVariant(first.mediaId(),"MASTER").orElseThrow();
        assertThat(variant.storageKey()).isEqualTo("images/fixture.jpg");
        assertThat(blobs.findByVariant(variant.variantId())).isEmpty();
        assertThat(BlobCatalogAdapter.digest(legacySource.read(variant.storageKey()))).isEqualTo(variant.checksum());
    }
    @Test void legacyBackfillDetectsChangedSourceWithoutRewritingTheCheckpoint() {
        var audit=new MediaAuditContext(MediaActorKind.SYSTEM,null,Instant.now(),null);
        String key="images/changed-fixture.jpg";var first=legacy.apply(key,audit);
        legacySource.changes.put(key,new byte[]{8,9,10});
        assertThat(legacy.inspect(key).state()).isEqualTo("SOURCE_CHANGED");
        assertThatThrownBy(() -> legacy.apply(key,audit)).isInstanceOf(IllegalStateException.class);
        assertThat(legacy.inspect(key).mediaId()).isEqualTo(first.mediaId());
    }
    @Test void referenceModerationDoesNotHideAnotherReferenceAndCanBeRestored() throws Exception {
        var a=upload.upload(66,MediaContext.IDENTITY_PROFILE,"66",image(6));
        long second=references.associate(66,a.mediaId(),MediaContext.IDENTITY_PROFILE,"66","GALLERY");
        governance.moderate(new ModerationDecision(a.mediaId(),a.referenceId(),ModerationDecision.State.HIDDEN,ModerationDecision.Reason.PRIVACY,"internal",66,Instant.now()));
        assertThatThrownBy(() -> resolution.resolve(66,a.mediaId(),a.referenceId(),"MASTER")).isInstanceOf(AccessDeniedException.class);
        assertThat(resolution.resolve(66,a.mediaId(),second,"MASTER").bytes()).isNotEmpty();
        governance.moderate(new ModerationDecision(a.mediaId(),a.referenceId(),ModerationDecision.State.VISIBLE,ModerationDecision.Reason.RESTORED,null,66,Instant.now()));
        assertThat(resolution.resolve(66,a.mediaId(),a.referenceId(),"MASTER").bytes()).isNotEmpty();
    }
    @Test void contextualBlockSurvivesReassociationUntilAuthorizedRestoration() throws Exception {
        var a=upload.upload(91,MediaContext.IDENTITY_PROFILE,"91",image(91));
        governance.moderate(new ModerationDecision(a.mediaId(),a.referenceId(),ModerationDecision.State.BLOCKED,ModerationDecision.Reason.PRIVACY,"scope",91,Instant.now()));
        references.dissociate(91,a.mediaId(),a.referenceId());
        long second=references.associate(91,a.mediaId(),MediaContext.IDENTITY_PROFILE,"91","PHOTO");
        assertThatThrownBy(() -> resolution.resolve(91,a.mediaId(),second,"MASTER")).isInstanceOf(AccessDeniedException.class);
        long separate=references.associate(91,a.mediaId(),MediaContext.IDENTITY_PROFILE,"91","GALLERY");
        assertThat(resolution.resolve(91,a.mediaId(),separate,"MASTER").bytes()).isNotEmpty();
        governance.moderate(new ModerationDecision(a.mediaId(),a.referenceId(),ModerationDecision.State.VISIBLE,ModerationDecision.Reason.RESTORED,null,91,Instant.now()));
        assertThat(resolution.resolve(91,a.mediaId(),second,"MASTER").bytes()).isNotEmpty();
    }
    @Test void moderationCannotResurrectDiscardedMedia() throws Exception {
        var a=upload.upload(92,MediaContext.IDENTITY_PROFILE,"92",image(92));references.dissociate(92,a.mediaId(),a.referenceId());
        admin.discard(a.mediaId(),new MediaAuditContext(MediaActorKind.USER,92L,Instant.now(),null));
        for(var state:List.of(ModerationDecision.State.VISIBLE,ModerationDecision.State.HIDDEN,ModerationDecision.State.BLOCKED)) {
            assertThatThrownBy(() -> governance.moderate(new ModerationDecision(a.mediaId(),null,state,state==ModerationDecision.State.VISIBLE?ModerationDecision.Reason.RESTORED:ModerationDecision.Reason.PRIVACY,null,92,Instant.now()))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> resolution.resolve(92,a.mediaId(),a.referenceId(),"MASTER")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> references.associate(92,a.mediaId(),MediaContext.IDENTITY_PROFILE,"92","PHOTO")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void backfillReappliesSameAssetAfterRollbackAndRejectsChangedSource() throws Exception {
        var audit=new MediaAuditContext(MediaActorKind.SYSTEM,null,Instant.now(),null);String key="images/reapply.jpg";
        var first=legacy.apply(key,audit);legacy.rollback(key,audit);
        var pool=Executors.newFixedThreadPool(2);
        try {
            var a=pool.submit(() -> legacy.apply(key,audit));var b=pool.submit(() -> legacy.apply(key,audit));
            assertThat(a.get(20,TimeUnit.SECONDS).mediaId()).isEqualTo(first.mediaId());
            assertThat(b.get(20,TimeUnit.SECONDS).state()).isEqualTo("CATALOGUED");
        } finally {pool.shutdownNow();}
        assertThat(admin.review(first.mediaId(),audit).decision()).isEqualTo("LEGAL_HOLD");
        legacy.rollback(key,audit);legacySource.changes.put(key,new byte[]{91,92,93});
        assertThatThrownBy(() -> legacy.apply(key,audit)).isInstanceOf(IllegalStateException.class);
        assertThat(legacy.inspect(key).state()).isEqualTo("ROLLED_BACK");
    }
    @Test void holdRequiresPrivateJustificationForBothTransitions() throws Exception {
        var a=upload.upload(93,MediaContext.IDENTITY_PROFILE,"93",image(93));var audit=new MediaAuditContext(MediaActorKind.USER,93L,Instant.now(),null);
        assertThatThrownBy(() -> governance.legalHold(a.mediaId(),true," ",audit)).isInstanceOf(IllegalArgumentException.class);
        governance.legalHold(a.mediaId(),true,"Private retention justification",audit);
        assertThatThrownBy(() -> governance.legalHold(a.mediaId(),false,null,audit)).isInstanceOf(IllegalArgumentException.class);
        assertThat(admin.review(a.mediaId(),audit).decision()).isEqualTo("LEGAL_HOLD");
        governance.legalHold(a.mediaId(),false,"Private release justification",audit);
        assertThat(jdbc.queryForObject("select internal_note from storage_governance_events where media_id=? and operation='LEGAL_HOLD_RELEASED'",String.class,a.mediaId())).isEqualTo("Private release justification");
        assertThat(admin.events(a.mediaId(),0,100).toString()).doesNotContain("Private release justification");
    }
    @Test void historicalReferenceRemainsReadableWhenItsConsumerAuthorizesIt() throws Exception {
        var a=upload.upload(44,MediaContext.IDENTITY_PROFILE,"44",image(4));
        var now=Instant.now();catalog.endReference(catalog.findReference(a.referenceId()).orElseThrow().end(now,44L,"REPLACED"),new MediaAuditContext(MediaActorKind.USER,44L,now,null));
        assertThat(resolution.resolve(44,a.mediaId(),a.referenceId(),"AVATAR_SMALL").bytes()).isNotEmpty();
    }
}
