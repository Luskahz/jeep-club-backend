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
    MediaImageProcessingAdapter.class,MediaUploadService.class,MediaResolutionService.class,MediaReferenceService.class,MediaAdministrationAdapter.class,BlobInspectionAdapter.class,LegacyBackfillAdapter.class,SqlUploadAdmission.class})
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
            public UploadIdentity uploadIdentity(long actor,String id) { return new UploadIdentity(actor,MediaSubjectType.USER,actor); }
            public boolean mayAssociate(long actor,MediaAsset asset,String id,String role) {return mayUpload(actor,id) && Long.valueOf(actor).equals(asset.ownerUserId());}
            public boolean mayDissociate(long actor,MediaReference ref) {return mayUpload(actor,ref.consumerId());}
            public boolean mayRead(long actor,MediaReference ref,boolean master) { return mayUpload(actor,ref.consumerId()); }
        }; }
    }
    static class LegacyFixture implements LegacyMediaSource {
        final java.util.concurrent.ConcurrentMap<String,byte[]> changes=new ConcurrentHashMap<>();
        final java.util.Set<String> missing=java.util.concurrent.ConcurrentHashMap.newKeySet();
        final java.util.concurrent.atomic.AtomicInteger reads=new java.util.concurrent.atomic.AtomicInteger();
        public byte[] read(String key) {
            reads.incrementAndGet();
            if(missing.contains(key)) throw new com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException();
            return changes.getOrDefault(key,("legacy-fixture:"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8)).clone();
        }
        public Metadata identify(byte[] bytes) {
            return new com.jeepclub.backend.storage.infra.blob.LegacyMediaSourceAdapter(org.mockito.Mockito.mock(com.jeepclub.backend.shared.storage.FileStorage.class)).identify(bytes);
        }
    }
    @Autowired UploadAdmission admission;
    @Autowired LegacyFixture legacySource;
    @Autowired MediaUploadService upload; @Autowired MediaResolutionService resolution;
    @Autowired BlobCatalog blobs; @Autowired PhysicalBlobStore physical; @Autowired MediaCatalogRepository catalog;
    @Autowired MediaGovernance governance; @Autowired JdbcTemplate jdbc; @Autowired PlatformTransactionManager manager;
    @Autowired MediaAdministration admin; @Autowired MediaReferenceService references; @Autowired LegacyBackfill legacy; @Autowired BlobInspection inspection;
    long attach(long actor,long media,MediaContext context,String resource,String role) {
        return new TransactionTemplate(manager).execute(s -> references.associate(actor,media,context,resource,role));
    }
    long positionedAttach(long actor,long media,Integer position) {
        return new TransactionTemplate(manager).execute(s->references.associate(actor,media,MediaContext.IDENTITY_PROFILE,Long.toString(actor),"GALLERY",position));
    }
    @Test void orderedGallerySupportsReplacementAndIndependentPositions() throws Exception {
        var a=upload.upload(201,MediaContext.IDENTITY_PROFILE,"201",image(201));
        var b=upload.upload(201,MediaContext.IDENTITY_PROFILE,"201",image(202));
        var c=upload.upload(201,MediaContext.IDENTITY_PROFILE,"201",image(203));
        long zero=positionedAttach(201,a.mediaId(),0),one=positionedAttach(201,b.mediaId(),1),two=positionedAttach(201,c.mediaId(),2);
        assertThat(jdbc.queryForList("select position from storage_media_references where consumer_id='201' and role='GALLERY' and unlinked_at is null order by position",Integer.class)).containsExactly(0,1,2);
        assertThat(positionedAttach(201,b.mediaId(),1)).isEqualTo(one);
        long repeatedAsset=positionedAttach(201,a.mediaId(),3);
        assertThat(repeatedAsset).isNotEqualTo(zero);
        var d=upload.upload(201,MediaContext.IDENTITY_PROFILE,"201",image(204));
        long replacement=new TransactionTemplate(manager).execute(s->references.replace(201,one,d.mediaId(),MediaContext.IDENTITY_PROFILE,"201","GALLERY",1));
        assertThat(catalog.findReference(replacement).orElseThrow().position()).isEqualTo(1);
        var history=catalog.findReference(one).orElseThrow();
        assertThat(history.position()).isEqualTo(1);assertThat(history.unlinkReasonCode()).isEqualTo("REPLACED");
        assertThat(history.isCurrent()).isFalse();assertThat(catalog.findReference(two).orElseThrow().isCurrent()).isTrue();
        assertThat(positionedAttach(201,b.mediaId(),1)).isNotEqualTo(one);
        assertThatThrownBy(()->new TransactionTemplate(manager).execute(s->references.replace(201,one,d.mediaId(),MediaContext.IDENTITY_PROFILE,"201","GALLERY",1)))
            .isInstanceOfAny(jakarta.persistence.OptimisticLockException.class,org.springframework.orm.ObjectOptimisticLockingFailureException.class);
    }
    @Test void logicalReorderingKeepsOldPositionAndCreatesNewInterval() throws Exception {
        var a=upload.upload(202,MediaContext.IDENTITY_PROFILE,"202",image(205));
        long old=positionedAttach(202,a.mediaId(),0);
        long reordered=new TransactionTemplate(manager).execute(s->references.replace(202,old,a.mediaId(),MediaContext.IDENTITY_PROFILE,"202","GALLERY",2));
        assertThat(reordered).isNotEqualTo(old);
        assertThat(catalog.findReference(old).orElseThrow().position()).isZero();
        assertThat(catalog.findReference(old).orElseThrow().isCurrent()).isFalse();
        assertThat(catalog.findReference(reordered).orElseThrow().position()).isEqualTo(2);
        assertThat(catalog.findReference(reordered).orElseThrow().isCurrent()).isTrue();
        assertThat(positionedAttach(202,a.mediaId(),2)).isEqualTo(reordered);
        long unpositioned=attach(202,a.mediaId(),MediaContext.IDENTITY_PROFILE,"202","PHOTO");
        assertThat(catalog.findReference(unpositioned).orElseThrow().position()).isNull();
    }
    @Test void negativePositionAndMissingConsumerTransactionAreRejected() throws Exception {
        var a=upload.upload(203,MediaContext.IDENTITY_PROFILE,"203",image(206));
        assertThatThrownBy(()->positionedAttach(203,a.mediaId(),-1)).isInstanceOf(IllegalArgumentException.class);
        long old=positionedAttach(203,a.mediaId(),0);
        assertThatThrownBy(()->new TransactionTemplate(manager).execute(s->references.replace(203,old,a.mediaId(),MediaContext.IDENTITY_PROFILE,"203","GALLERY",-1)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(catalog.findReference(old).orElseThrow().isCurrent()).isTrue();
        assertThatThrownBy(()->references.associate(203,a.mediaId(),MediaContext.IDENTITY_PROFILE,"203","GALLERY",1))
            .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }
    @Test void concurrentIdenticalPositionAssociationReturnsOneReferenceAndAudit() throws Exception {
        var a=upload.upload(204,MediaContext.IDENTITY_PROFILE,"204",image(207));
        var pool=Executors.newFixedThreadPool(4);var start=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<Long>>();
            for(int i=0;i<4;i++) futures.add(pool.submit(()->{start.await();return positionedAttach(204,a.mediaId(),1);}));
            start.countDown();var ids=new HashSet<Long>();for(var future:futures) ids.add(future.get(20,TimeUnit.SECONDS));
            assertThat(ids).hasSize(1);assertThat(catalog.findReferences(a.mediaId(),0,100)).hasSize(1);
            assertThat(catalog.findAuditEvents(a.mediaId(),0,100).stream().filter(e->e.operation()==MediaAuditOperation.REFERENCE_RECORDED)).hasSize(1);
        } finally {pool.shutdownNow();}
    }
    @Test void positionedReplacementAndAssociationRollBackTogether() throws Exception {
        var a=upload.upload(205,MediaContext.IDENTITY_PROFILE,"205",image(208));
        var b=upload.upload(205,MediaContext.IDENTITY_PROFILE,"205",image(209));
        long old=positionedAttach(205,a.mediaId(),1);int auditCount=catalog.findAuditEvents(a.mediaId(),0,100).size();
        assertThatThrownBy(()->new TransactionTemplate(manager).execute(s->{
            references.replace(205,old,b.mediaId(),MediaContext.IDENTITY_PROFILE,"205","GALLERY",1);
            references.associate(205,b.mediaId(),MediaContext.IDENTITY_PROFILE,"205","GALLERY",2);
            throw new IllegalStateException("aggregate save failed");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(catalog.findReference(old).orElseThrow().isCurrent()).isTrue();
        assertThat(catalog.findReference(old).orElseThrow().position()).isEqualTo(1);
        assertThat(catalog.findReferences(b.mediaId(),0,100)).isEmpty();
        assertThat(catalog.findAuditEvents(a.mediaId(),0,100)).hasSize(auditCount);
    }
    @Test void orderedReferencesDoNotShareAuthorizationThroughPhysicalDeduplication() throws Exception {
        byte[] bytes=image(210);
        var a=upload.upload(206,MediaContext.IDENTITY_PROFILE,"206",bytes);
        var b=upload.upload(207,MediaContext.IDENTITY_PROFILE,"207",bytes);
        long first=positionedAttach(206,a.mediaId(),0),second=positionedAttach(207,b.mediaId(),2);
        assertThat(blobs.findByVariant(catalog.findVariant(a.mediaId(),"MASTER").orElseThrow().variantId()))
            .isEqualTo(blobs.findByVariant(catalog.findVariant(b.mediaId(),"MASTER").orElseThrow().variantId()));
        assertThat(resolution.resolve(206,a.mediaId(),first,"MASTER").bytes()).isNotEmpty();
        assertThat(resolution.resolve(207,b.mediaId(),second,"MASTER").bytes()).isNotEmpty();
        assertThatThrownBy(()->resolution.resolve(207,a.mediaId(),first,"MASTER")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->positionedAttach(207,a.mediaId(),2)).isInstanceOf(AccessDeniedException.class);
    }
    void detach(long actor,long media,long ref) {
        new TransactionTemplate(manager).executeWithoutResult(s -> references.dissociate(actor,media,ref));
    }
    MediaUploadService.Uploaded attachedUpload(long actor,MediaContext context,String resource,byte[] bytes) {
        var uploaded=upload.upload(actor,context,resource,bytes);
        assertThat(uploaded.referenceId()).isNull();
        return new MediaUploadService.Uploaded(uploaded.mediaId(),attach(actor,uploaded.mediaId(),context,resource,"PHOTO"));
    }
    byte[] image(int color) throws Exception {
        var image=new BufferedImage(80,60,BufferedImage.TYPE_INT_RGB);image.setRGB(1,1,color);
        var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();
    }
    @Test void identicalUploadsHaveIndependentIdentityAndSharePhysicalRepresentations() throws Exception {
        var a=attachedUpload(11,MediaContext.IDENTITY_PROFILE,"11",image(1));
        var b=attachedUpload(22,MediaContext.IDENTITY_PROFILE,"22",image(1));
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
    @Test void unattachedUploadHasNoActiveReferenceAndRetentionStartsAtUpload() throws Exception {
        var a=upload.upload(101,MediaContext.IDENTITY_PROFILE,"101",image(101));
        assertThat(a.referenceId()).isNull();
        assertThat(catalog.findReferences(a.mediaId(),0,100)).isEmpty();
        var asset=catalog.findAsset(a.mediaId()).orElseThrow();
        var review=admin.review(a.mediaId(),new MediaAuditContext(MediaActorKind.USER,101L,Instant.now(),null));
        assertThat(review.decision()).isEqualTo("WAITING");
        assertThat(review.unusedSince()).isEqualTo(asset.createdAt());
    }
    @Test void associationIsIdempotentAndRollsBackWithConsumerTransaction() throws Exception {
        var a=upload.upload(102,MediaContext.IDENTITY_PROFILE,"102",image(102));
        long first=attach(102,a.mediaId(),MediaContext.IDENTITY_PROFILE,"102","PHOTO");
        assertThat(attach(102,a.mediaId(),MediaContext.IDENTITY_PROFILE,"102","PHOTO")).isEqualTo(first);
        assertThat(catalog.findReferences(a.mediaId(),0,100)).hasSize(1);
        assertThatThrownBy(() -> new TransactionTemplate(manager).execute(s -> {
            references.dissociate(102,a.mediaId(),first);
            throw new IllegalStateException("consumer rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(catalog.findReference(first).orElseThrow().isCurrent()).isTrue();
        assertThatThrownBy(() -> references.associate(102,a.mediaId(),MediaContext.IDENTITY_PROFILE,"102","PHOTO"))
            .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
        assertThatThrownBy(() -> attach(103,a.mediaId(),MediaContext.IDENTITY_PROFILE,"103","PHOTO"))
            .isInstanceOf(AccessDeniedException.class);
    }
    @Test void replacementPreservesHistoryAndRollsBackAtomically() throws Exception {
        var a=attachedUpload(104,MediaContext.IDENTITY_PROFILE,"104",image(104));
        var b=upload.upload(104,MediaContext.IDENTITY_PROFILE,"104",image(105));
        assertThatThrownBy(() -> new TransactionTemplate(manager).execute(s -> {
            references.replace(104,a.referenceId(),b.mediaId(),MediaContext.IDENTITY_PROFILE,"104","PHOTO");
            throw new IllegalStateException("consumer save failed");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(catalog.findReference(a.referenceId()).orElseThrow().isCurrent()).isTrue();
        assertThat(catalog.findReferences(b.mediaId(),0,100)).isEmpty();
        long next=new TransactionTemplate(manager).execute(s -> references.replace(104,a.referenceId(),b.mediaId(),MediaContext.IDENTITY_PROFILE,"104","PHOTO"));
        var old=catalog.findReference(a.referenceId()).orElseThrow();
        assertThat(old.unlinkReasonCode()).isEqualTo("REPLACED");
        assertThat(old.unlinkedAt()).isNotNull();
        assertThat(catalog.findReference(next).orElseThrow().isCurrent()).isTrue();
        assertThat(blobs.findByVariant(catalog.findVariant(a.mediaId(),"MASTER").orElseThrow().variantId())).isPresent();
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
        var service=new MediaUploadService(catalog,blobs,broken,governance,List.of(new Configuration().policy()),manager,new SimpleMeterRegistry(),admission);
        assertThatThrownBy(() -> service.upload(77,MediaContext.IDENTITY_PROFILE,"77",bytes)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("select count(*) from storage_media_assets where owner_user_id=77",Long.class)).isZero();
        var hash=BlobCatalogAdapter.digest(bytes);
        assertThat(jdbc.queryForObject("select count(*) from storage_blobs where sha256=?",Long.class,hash)).isEqualTo(1);
        assertThat(physical.readVerified(hash,bytes.length)).containsExactly(bytes);
    }
    @Test void retentionHoldDiscardAndSensitiveHistoryArePersisted() throws Exception {
        var a=attachedUpload(55,MediaContext.IDENTITY_PROFILE,"55",image(5));
        Instant now=Instant.now();var audit=new MediaAuditContext(MediaActorKind.USER,55L,now,null);
        assertThatThrownBy(() -> admin.discard(a.mediaId(),audit)).isInstanceOf(IllegalArgumentException.class);
        var sensitive=catalog.recordReference(new MediaReference(null,a.mediaId(),MediaContext.IDENTIFICATION_PHOTO,"55","CARD",null,MediaOrigin.UPLOAD,now,55L,null,null,null,null),audit);
        detach(55,a.mediaId(),a.referenceId());
        now=Instant.now();catalog.endReference(sensitive.end(now,55L,"END"),new MediaAuditContext(MediaActorKind.USER,55L,now,null));
        assertThat(admin.review(a.mediaId(),new MediaAuditContext(MediaActorKind.USER,55L,now.plusSeconds(91L*86400),null)).retentionClass()).isEqualTo("SENSITIVE");
        governance.legalHold(a.mediaId(),true,"Retention fixture obligation",audit);
        assertThat(admin.review(a.mediaId(),audit).decision()).isEqualTo("LEGAL_HOLD");
        admin.discard(a.mediaId(),audit);
        assertThatThrownBy(() -> resolution.resolve(55,a.mediaId(),a.referenceId(),"MASTER")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> attach(55,a.mediaId(),MediaContext.IDENTITY_PROFILE,"55","PHOTO")).isInstanceOf(IllegalArgumentException.class);
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
    @Test void backfillConfirmsPresenceAndResolvesOnlyAuthorizedReference() throws Exception {
        var audit=new MediaAuditContext(MediaActorKind.SYSTEM,null,Instant.now(),null);
        String key="images/physical-regression.jpg";
        legacySource.changes.put(key,image(111));
        var result=legacy.apply(key,audit);
        var asset=catalog.findAsset(result.mediaId()).orElseThrow();
        assertThat(asset.physicalState()).isEqualTo(MediaPhysicalState.PRESENT);
        assertThat(asset.ownerUserId()).isNull();
        var now=Instant.now();
        var ref=catalog.recordReference(new MediaReference(null,result.mediaId(),MediaContext.IDENTITY_PROFILE,"111","PHOTO",null,MediaOrigin.LEGACY_IMPORT,now,null,null,null,null,null),
            new MediaAuditContext(MediaActorKind.SYSTEM,null,now,null));
        assertThat(resolution.resolve(111,result.mediaId(),ref.referenceId(),"MASTER").bytes()).containsExactly(legacySource.read(key));
        assertThatThrownBy(()->resolution.resolve(112,result.mediaId(),ref.referenceId(),"MASTER")).isInstanceOf(AccessDeniedException.class);
        assertThat(legacy.apply(key,new MediaAuditContext(MediaActorKind.SYSTEM,null,Instant.now(),null)).mediaId()).isEqualTo(result.mediaId());
        int reads=legacySource.reads.get();
        var noPolicy=new MediaResolutionService(catalog,blobs,governance,List.of(),legacySource);
        assertThatThrownBy(()->noPolicy.resolve(111,result.mediaId(),ref.referenceId(),"MASTER")).isInstanceOf(AccessDeniedException.class);
        assertThat(legacySource.reads.get()).isEqualTo(reads);
        legacySource.changes.put(key,image(112));
        assertThatThrownBy(()->resolution.resolve(111,result.mediaId(),ref.referenceId(),"MASTER")).isInstanceOf(MediaIntegrityException.class);
        legacySource.missing.add(key);
        assertThatThrownBy(()->resolution.resolve(111,result.mediaId(),ref.referenceId(),"MASTER"))
            .isInstanceOf(com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException.class);
    }
    @Test void unknownOrCorruptLegacyContentCannotBecomePresent() {
        var result=legacy.apply("images/not-an-image.jpg",systemAudit());
        assertThat(catalog.findAsset(result.mediaId()).orElseThrow().physicalState()).isEqualTo(MediaPhysicalState.UNKNOWN);
        String missing="images/absent-regression.jpg";legacySource.missing.add(missing);
        assertThatThrownBy(()->legacy.apply(missing,systemAudit())).isInstanceOf(com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException.class);
    }
    MediaAuditContext systemAudit() {return new MediaAuditContext(MediaActorKind.SYSTEM,null,Instant.now(),null);}
    @Test void realBackfillFilesystemAndAuthorizedHttpGetPreserveLegacyFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path root) throws Exception {
        String key="images/2026/11/12/550e8400-e29b-41d4-a716-446655440001.png";byte[] bytes=image(114);
        var file=root.resolve(key);Files.createDirectories(file.getParent());Files.write(file,bytes);
        var modified=Files.getLastModifiedTime(file);
        var properties=new StorageProperties();properties.local().setRootDirectory(root);
        var source=new com.jeepclub.backend.storage.infra.blob.LegacyMediaSourceAdapter(
            new com.jeepclub.backend.platform.storage.local.LocalFileStorage(properties,java.time.Clock.systemUTC()));
        var importer=new LegacyBackfillAdapter(quotaEntityManager,blobs,source,catalog,governance,admin,manager,true);
        long id=importer.apply(key,systemAudit()).mediaId();
        assertThat(catalog.findAsset(id).orElseThrow().physicalState()).isEqualTo(MediaPhysicalState.PRESENT);
        var audit=systemAudit();var ref=catalog.recordReference(new MediaReference(null,id,MediaContext.IDENTITY_PROFILE,"114","PHOTO",null,
            MediaOrigin.LEGACY_IMPORT,audit.occurredAt(),null,null,null,null,null),audit);
        var resolver=new MediaResolutionService(catalog,blobs,governance,List.of(new Configuration().policy()),source);
        var controller=new com.jeepclub.backend.storage.api.http.StorageMediaController(upload,resolver,references);
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                public boolean supportsParameter(org.springframework.core.MethodParameter p) {return p.getParameterType()==com.jeepclub.backend.platform.security.principal.UserPrincipal.class;}
                public Object resolveArgument(org.springframework.core.MethodParameter p,org.springframework.web.method.support.ModelAndViewContainer m,
                    org.springframework.web.context.request.NativeWebRequest r,org.springframework.web.bind.support.WebDataBinderFactory b) {
                    return new com.jeepclub.backend.platform.security.principal.UserPrincipal(114L,1L,null,Instant.now().plusSeconds(60));
                }
            }).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/storage/media/"+id+"/references/"+ref.referenceId()+"/variants/MASTER"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(bytes));
        assertThat(Files.readAllBytes(file)).containsExactly(bytes);assertThat(Files.getLastModifiedTime(file)).isEqualTo(modified);
        assertThat(importer.apply(key,systemAudit()).mediaId()).isEqualTo(id);
    }
    MediaAsset physicalCandidate(String key,byte[] bytes) {
        var result=legacy.apply(key,systemAudit());
        return catalog.findAsset(result.mediaId()).orElseThrow();
    }
    @Test void physicalConfirmationChecksDigestVersionAndAuditAtomicity() {
        var asset=physicalCandidate("images/proof-audit.jpg",new byte[]{1});
        var variant=catalog.findVariant(asset.mediaId(),"MASTER").orElseThrow();
        byte[] proof=legacySource.read(variant.storageKey());
        assertThatThrownBy(()->catalog.confirmPhysicalPresence(asset.mediaId(),asset.version(),variant.variantId(),new byte[]{1},systemAudit())).isInstanceOf(IllegalArgumentException.class);
        assertThat(catalog.findAsset(asset.mediaId()).orElseThrow().physicalState()).isEqualTo(MediaPhysicalState.UNKNOWN);
        String constraint="ck_r61_physical_audit_failure";
        jdbc.execute("alter table storage_governance_events add constraint "+constraint+" check (media_id <> "+asset.mediaId()+" or operation <> 'PHYSICAL_PRESENCE_CONFIRMED')");
        try {
            assertThatThrownBy(()->catalog.confirmPhysicalPresence(asset.mediaId(),asset.version(),variant.variantId(),proof,systemAudit())).isInstanceOf(RuntimeException.class);
            assertThat(catalog.findAsset(asset.mediaId()).orElseThrow()).isEqualTo(asset);
            assertThat(catalog.findAuditEvents(asset.mediaId(),0,100)).noneMatch(e->e.operation()==MediaAuditOperation.ASSET_METADATA_UPDATED);
        } finally {
            boolean mysql=jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>)c->c.getMetaData().getDatabaseProductName().contains("MySQL"));
            jdbc.execute("alter table storage_governance_events drop "+(mysql?"check ":"constraint ")+constraint);
        }
        var confirmed=catalog.confirmPhysicalPresence(asset.mediaId(),asset.version(),variant.variantId(),proof,systemAudit());
        assertThat(confirmed.physicalState()).isEqualTo(MediaPhysicalState.PRESENT);
        assertThat(confirmed.version()).isEqualTo(asset.version()+1);
        assertThatThrownBy(()->catalog.confirmPhysicalPresence(asset.mediaId(),asset.version(),variant.variantId(),proof,systemAudit())).isInstanceOf(jakarta.persistence.OptimisticLockException.class);
    }
    @Test void concurrentPhysicalConfirmationsHaveOneVersionedWinner() throws Exception {
        var asset=physicalCandidate("images/proof-concurrent.jpg",new byte[]{1});
        var variant=catalog.findVariant(asset.mediaId(),"MASTER").orElseThrow();byte[] proof=legacySource.read(variant.storageKey());
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            var futures=new ArrayList<Future<Boolean>>();
            for(int i=0;i<2;i++) futures.add(pool.submit(()->{start.await();try {
                catalog.confirmPhysicalPresence(asset.mediaId(),asset.version(),variant.variantId(),proof,systemAudit());return true;
            } catch(org.springframework.orm.ObjectOptimisticLockingFailureException | jakarta.persistence.OptimisticLockException conflict) {return false;}}));
            start.countDown();int winners=0;for(var future:futures) if(future.get(20,TimeUnit.SECONDS)) winners++;
            assertThat(winners).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from storage_governance_events where media_id=? and operation='PHYSICAL_PRESENCE_CONFIRMED'",Long.class,asset.mediaId())).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }
    @Test void verifiedBackfillReapplicationPreservesModerationHoldAndIdentity() throws Exception {
        String key="images/proof-reapply.jpg";legacySource.changes.put(key,image(113));
        long id=legacy.apply(key,systemAudit()).mediaId();
        governance.moderate(new ModerationDecision(id,null,ModerationDecision.State.BLOCKED,ModerationDecision.Reason.PRIVACY,"fixture",113,Instant.now()));
        legacy.rollback(key,systemAudit());
        assertThat(legacy.apply(key,systemAudit()).mediaId()).isEqualTo(id);
        assertThat(catalog.findAsset(id).orElseThrow().physicalState()).isEqualTo(MediaPhysicalState.PRESENT);
        assertThat(admin.list(0,100,MediaContext.UNKNOWN,"BLOCKED")).extracting(MediaAdministration.Entry::mediaId).contains(id);
        assertThat(admin.review(id,systemAudit()).decision()).isEqualTo("LEGAL_HOLD");
        assertThat(jdbc.queryForObject("select count(*) from storage_media_variants where media_id=?",Long.class,id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from storage_legacy_checkpoints where media_id=?",Long.class,id)).isEqualTo(1);
    }
    @Test void referenceModerationDoesNotHideAnotherReferenceAndCanBeRestored() throws Exception {
        var a=attachedUpload(66,MediaContext.IDENTITY_PROFILE,"66",image(6));
        long second=attach(66,a.mediaId(),MediaContext.IDENTITY_PROFILE,"66","GALLERY");
        governance.moderate(new ModerationDecision(a.mediaId(),a.referenceId(),ModerationDecision.State.HIDDEN,ModerationDecision.Reason.PRIVACY,"internal",66,Instant.now()));
        assertThatThrownBy(() -> resolution.resolve(66,a.mediaId(),a.referenceId(),"MASTER")).isInstanceOf(AccessDeniedException.class);
        assertThat(resolution.resolve(66,a.mediaId(),second,"MASTER").bytes()).isNotEmpty();
        governance.moderate(new ModerationDecision(a.mediaId(),a.referenceId(),ModerationDecision.State.VISIBLE,ModerationDecision.Reason.RESTORED,null,66,Instant.now()));
        assertThat(resolution.resolve(66,a.mediaId(),a.referenceId(),"MASTER").bytes()).isNotEmpty();
    }
    @Test void contextualBlockSurvivesReassociationUntilAuthorizedRestoration() throws Exception {
        var a=attachedUpload(91,MediaContext.IDENTITY_PROFILE,"91",image(91));
        governance.moderate(new ModerationDecision(a.mediaId(),a.referenceId(),ModerationDecision.State.BLOCKED,ModerationDecision.Reason.PRIVACY,"scope",91,Instant.now()));
        detach(91,a.mediaId(),a.referenceId());
        long second=attach(91,a.mediaId(),MediaContext.IDENTITY_PROFILE,"91","PHOTO");
        assertThatThrownBy(() -> resolution.resolve(91,a.mediaId(),second,"MASTER")).isInstanceOf(AccessDeniedException.class);
        long separate=attach(91,a.mediaId(),MediaContext.IDENTITY_PROFILE,"91","GALLERY");
        assertThat(resolution.resolve(91,a.mediaId(),separate,"MASTER").bytes()).isNotEmpty();
        governance.moderate(new ModerationDecision(a.mediaId(),a.referenceId(),ModerationDecision.State.VISIBLE,ModerationDecision.Reason.RESTORED,null,91,Instant.now()));
        assertThat(resolution.resolve(91,a.mediaId(),second,"MASTER").bytes()).isNotEmpty();
    }
    @Test void moderationCannotResurrectDiscardedMedia() throws Exception {
        var a=attachedUpload(92,MediaContext.IDENTITY_PROFILE,"92",image(92));detach(92,a.mediaId(),a.referenceId());
        admin.discard(a.mediaId(),new MediaAuditContext(MediaActorKind.USER,92L,Instant.now(),null));
        for(var state:List.of(ModerationDecision.State.VISIBLE,ModerationDecision.State.HIDDEN,ModerationDecision.State.BLOCKED)) {
            assertThatThrownBy(() -> governance.moderate(new ModerationDecision(a.mediaId(),null,state,state==ModerationDecision.State.VISIBLE?ModerationDecision.Reason.RESTORED:ModerationDecision.Reason.PRIVACY,null,92,Instant.now()))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> resolution.resolve(92,a.mediaId(),a.referenceId(),"MASTER")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> attach(92,a.mediaId(),MediaContext.IDENTITY_PROFILE,"92","PHOTO")).isInstanceOf(IllegalArgumentException.class);
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
        var a=attachedUpload(93,MediaContext.IDENTITY_PROFILE,"93",image(93));var audit=new MediaAuditContext(MediaActorKind.USER,93L,Instant.now(),null);
        assertThatThrownBy(() -> governance.legalHold(a.mediaId(),true," ",audit)).isInstanceOf(IllegalArgumentException.class);
        governance.legalHold(a.mediaId(),true,"Private retention justification",audit);
        assertThatThrownBy(() -> governance.legalHold(a.mediaId(),false,null,audit)).isInstanceOf(IllegalArgumentException.class);
        assertThat(admin.review(a.mediaId(),audit).decision()).isEqualTo("LEGAL_HOLD");
        governance.legalHold(a.mediaId(),false,"Private release justification",audit);
        assertThat(jdbc.queryForObject("select internal_note from storage_governance_events where media_id=? and operation='LEGAL_HOLD_RELEASED'",String.class,a.mediaId())).isEqualTo("Private release justification");
        assertThat(admin.events(a.mediaId(),0,100).toString()).doesNotContain("Private release justification");
    }
    @Test void uploadAdmissionSerializesPerUserConcurrencyAndRetainsCustodyCharges() {
        var a=admission.reserve(94);var b=admission.reserve(94);
        assertThatThrownBy(() -> admission.reserve(94)).isInstanceOf(UploadAdmission.LimitExceeded.class);
        admission.finish(a);var c=admission.reserve(94);admission.charge(c,1000);admission.finish(c);admission.finish(c);admission.finish(b);
        assertThat(jdbc.queryForObject("select used_bytes from storage_upload_budgets where scope_id=94",Long.class)).isEqualTo(1000);
        assertThat(jdbc.queryForObject("select in_flight from storage_upload_budgets where scope_id=94",Integer.class)).isZero();
    }
    @Test void hourlyAndLifetimeLimitsRejectBeforeProcessingOrStorage() {
        var limited=new SqlUploadAdmission(emForQuota(),manager,64L*1024*1024,100L*1024*1024,1,1,10);
        var permit=limited.reserve(95);limited.finish(permit);
        assertThatThrownBy(() -> limited.reserve(95)).isInstanceOf(UploadAdmission.LimitExceeded.class);
    }
    @Test void byteAndRetainedUploadBudgetsRemainIndependentOfHourlyAllowance() {
        var byteLimited=new SqlUploadAdmission(emForQuota(),manager,20L*1024*1024,100L*1024*1024,30,2,10);
        var pending=byteLimited.reserve(96);assertThatThrownBy(() -> byteLimited.reserve(96)).isInstanceOf(UploadAdmission.LimitExceeded.class);byteLimited.finish(pending);
        var retained=new SqlUploadAdmission(emForQuota(),manager,64L*1024*1024,100L*1024*1024,30,2,1);
        var charged=retained.reserve(97);retained.charge(charged,1000);retained.finish(charged);
        assertThatThrownBy(() -> retained.reserve(97)).isInstanceOf(UploadAdmission.LimitExceeded.class);
    }
    @Autowired jakarta.persistence.EntityManager quotaEntityManager;
    private jakarta.persistence.EntityManager emForQuota() {return quotaEntityManager;}
    @Test void reconciliationBatchesSqlRegardlessOfPageSize() {
        var stats=quotaEntityManager.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();boolean previous=stats.isStatisticsEnabled();stats.setStatisticsEnabled(true);
        try {
            for(int i=1;i<=12;i++) blobs.store(new byte[]{(byte)(100+i)});
            stats.clear();inspection.inspect("",1);long small=stats.getPrepareStatementCount();
            stats.clear();inspection.inspect("",12);long large=stats.getPrepareStatementCount();
            assertThat(small).isBetween(1L,3L);assertThat(large).isLessThanOrEqualTo(small);
            var fs=inspection.inspect("",2,null,true);assertThat(fs.uncataloguedFiles()).hasSizeLessThanOrEqualTo(2);
        } finally {stats.setStatisticsEnabled(previous);}
    }
    @Test void admissionRejectsBeforeReadingTheUploadPayload() {
        var a=admission.reserve(98);var b=admission.reserve(98);
        try {assertThatThrownBy(() -> upload.upload(98,MediaContext.IDENTITY_PROFILE,"98",() -> {throw new AssertionError("Payload read before admission");})).isInstanceOf(UploadAdmission.LimitExceeded.class);}
        finally {admission.finish(a);admission.finish(b);}
    }
    @Test void publishedWave3CopiedCheckpointIsReappliedWithoutCreatingAnotherAssetOrCopy() {
        String key="images/wave3-copy.jpg";byte[] bytes=legacySource.read(key);var blob=blobs.store(bytes);
        var audit=new MediaAuditContext(MediaActorKind.SYSTEM,null,Instant.now(),null);
        long id=new TransactionTemplate(manager).execute(status -> {
            String sourceId=BlobCatalogAdapter.digest(("LOCAL\0"+key).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var asset=catalog.registerAsset(MediaAsset.create(MediaOrigin.LEGACY_IMPORT,MediaContext.UNKNOWN,sourceId,null,null,MediaSubjectType.UNKNOWN,null,"LEGACY_CUSTODY",audit.occurredAt()),audit);
            var variant=catalog.registerVariant(new MediaVariant(null,asset.mediaId(),"MASTER",MediaVariantRole.MASTER,"LOCAL","storage-blobs",blob.storageKey(),"application/octet-stream",bytes.length,null,null,"SHA-256",blob.sha256(),null,"LEGACY_V1",null,audit.occurredAt()),audit);
            blobs.bind(variant.variantId(),blob);governance.initialize(asset.mediaId(),blob.sha256(),true,audit);
            governance.legalHold(asset.mediaId(),true,"Published legacy custody",audit);
            var checkpoint=new com.jeepclub.backend.storage.infra.persistence.entity.LegacyMediaCheckpointEntity();checkpoint.setSourceId(sourceId);checkpoint.setLegacyKey(key);checkpoint.setMediaId(asset.mediaId());quotaEntityManager.persist(checkpoint);
            return asset.mediaId();
        });
        legacy.rollback(key,audit);assertThat(legacy.apply(key,audit).mediaId()).isEqualTo(id);
        assertThat(catalog.findVariant(id,"MASTER").orElseThrow().storageKey()).isEqualTo(blob.storageKey());
        assertThat(jdbc.queryForObject("select count(*) from storage_media_variants where media_id=?",Long.class,id)).isEqualTo(1);
        assertThat(blobs.read(blob)).containsExactly(bytes);
    }
    @Test void realLegacySourcePersistsVerifiedMimeAndDimensionsWithoutCopy() throws Exception {
        String key="images/2026/11/09/550e8400-e29b-41d4-a716-446655440000.jpg";byte[] bytes=image(99);
        var provider=org.mockito.Mockito.mock(com.jeepclub.backend.shared.storage.FileStorage.class);
        org.mockito.Mockito.when(provider.load(key)).thenReturn(new com.jeepclub.backend.shared.storage.StorageResource(key,bytes));
        var source=new com.jeepclub.backend.storage.infra.blob.LegacyMediaSourceAdapter(provider);
        var importer=new LegacyBackfillAdapter(quotaEntityManager,blobs,source,catalog,governance,admin,manager,true);
        long before=jdbc.queryForObject("select count(*) from storage_blobs",Long.class);
        var result=importer.apply(key,new MediaAuditContext(MediaActorKind.SYSTEM,null,Instant.now(),null));
        var variant=catalog.findVariant(result.mediaId(),"MASTER").orElseThrow();
        assertThat(variant.storageKey()).isEqualTo(key);assertThat(variant.mimeType()).isEqualTo("image/png");
        assertThat(variant.width()).isEqualTo(80);assertThat(variant.height()).isEqualTo(60);
        assertThat(jdbc.queryForObject("select count(*) from storage_blobs",Long.class)).isEqualTo(before);
        org.mockito.Mockito.verify(provider).load(key);org.mockito.Mockito.verifyNoMoreInteractions(provider);
    }
    @Test void concurrentUploadAdmissionNeverExceedsTheSharedUserLimit() throws Exception {
        var warmup=admission.reserve(99);admission.finish(warmup);
        var pool=Executors.newFixedThreadPool(4);var start=new CountDownLatch(1);var granted=new ArrayList<UploadAdmission.Permit>();
        try {
            var futures=new ArrayList<Future<UploadAdmission.Permit>>();
            for(int i=0;i<4;i++) futures.add(pool.submit(() -> {start.await();try{return admission.reserve(99);}catch(UploadAdmission.LimitExceeded denied){return null;}}));
            start.countDown();for(var future:futures){var permit=future.get(20,TimeUnit.SECONDS);if(permit!=null) granted.add(permit);}
            assertThat(granted).hasSize(2);
            assertThat(jdbc.queryForObject("select in_flight from storage_upload_budgets where scope_id=99",Integer.class)).isEqualTo(2);
        } finally {for(var permit:granted) admission.finish(permit);pool.shutdownNow();}
        assertThat(jdbc.queryForObject("select in_flight from storage_upload_budgets where scope_id=99",Integer.class)).isZero();
    }
    @Test void historicalReferenceRemainsReadableWhenItsConsumerAuthorizesIt() throws Exception {
        var a=attachedUpload(44,MediaContext.IDENTITY_PROFILE,"44",image(4));
        var now=Instant.now();catalog.endReference(catalog.findReference(a.referenceId()).orElseThrow().end(now,44L,"REPLACED"),new MediaAuditContext(MediaActorKind.USER,44L,now,null));
        assertThat(resolution.resolve(44,a.mediaId(),a.referenceId(),"AVATAR_SMALL").bytes()).isNotEmpty();
    }
}
