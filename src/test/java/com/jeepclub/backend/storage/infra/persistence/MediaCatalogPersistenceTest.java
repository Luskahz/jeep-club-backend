package com.jeepclub.backend.storage.infra.persistence;

import com.jeepclub.backend.storage.core.domain.enums.*;
import com.jeepclub.backend.storage.core.domain.model.*;
import com.jeepclub.backend.storage.core.repository.MediaCatalogRepository;
import com.jeepclub.backend.storage.infra.persistence.adapter.MediaCatalogRepositoryAdapter;
import com.jeepclub.backend.storage.infra.persistence.entity.*;
import com.jeepclub.backend.storage.infra.persistence.mapper.MediaCatalogMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;

@org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase(replace = org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@ContextConfiguration(classes = MediaCatalogPersistenceTest.Configuration.class)
@Import(MediaCatalogRepositoryAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MediaCatalogPersistenceTest {
    static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");
    @TestConfiguration
    @org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.jeepclub.backend.storage.infra.persistence")
    @EntityScan(basePackageClasses = MediaAssetEntity.class)
    static class Configuration {}
    @Autowired MediaCatalogRepository catalog;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired JdbcTemplate jdbc;

    <T> T tx(Supplier<T> action) { return new TransactionTemplate(manager).execute(status -> action.get()); }
    MediaAuditContext audit() { return new MediaAuditContext(MediaActorKind.SYSTEM, null, NOW.plusSeconds(10), UUID.randomUUID().toString()); }
    MediaAsset asset() { return catalog.registerAsset(MediaAsset.create(MediaOrigin.UNKNOWN, MediaContext.UNKNOWN, null,
            null, null, MediaSubjectType.UNKNOWN, null, "UNKNOWN", NOW), audit()); }
    MediaVariant variant(Long media, String code, String key) {
        return new MediaVariant(null, media, code, code.equals("MASTER") ? MediaVariantRole.MASTER : MediaVariantRole.DERIVED,
                "LOCAL", "private-images", key, "image/jpeg", 120, null, null, null, null, null, null, null, NOW);
    }
    MediaReference reference(Long media, String consumer) {
        return new MediaReference(null, media, MediaContext.VEHICLE_PHOTO, consumer, "GALLERY", 0, MediaOrigin.UNKNOWN,
                NOW, null, null, null, null, null);
    }
    long events(Long media) { return jdbc.queryForObject("select count(*) from storage_media_audit_events where media_id=?", Long.class, media); }
    @Test void roundTripsUnknownAndKnownProvenanceWithoutCrossModuleForeignKeys() {
        var unknown = asset();
        assertThat(catalog.findAsset(unknown.mediaId())).contains(unknown);
        var known = catalog.registerAsset(MediaAsset.create(MediaOrigin.UPLOAD, MediaContext.IDENTITY_PROFILE, "synthetic-source",
                1101L, 2202L, MediaSubjectType.DEPENDENT, 3303L, "PROFILE", NOW), audit());
        assertThat(catalog.findAsset(known.mediaId())).contains(known);
        assertThat(events(unknown.mediaId())).isEqualTo(1);
        assertThat(events(known.mediaId())).isEqualTo(1);
    }
    @Test void multipleVariantsHaveOwnIdentityAndUniqueSemanticCodeAndPhysicalLocation() {
        var a = asset(); var b = asset(); String key = UUID.randomUUID()+"/Case.jpg";
        var master = catalog.registerVariant(variant(a.mediaId(), "MASTER", key), audit());
        assertThat(master.variantId()).isPositive(); assertThat(master.isPrivateSource()).isTrue();
        assertThat(catalog.findVariant(master.variantId())).contains(master);
        var derived = catalog.registerVariant(variant(a.mediaId(), "FUTURE_PRESET", key+".derived"), audit());
        assertThat(derived.variantId()).isNotEqualTo(master.variantId());
        assertThatThrownBy(() -> catalog.registerVariant(variant(a.mediaId(), "MASTER", key+"other"), audit())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> catalog.registerVariant(variant(b.mediaId(), "MASTER", key), audit())).isInstanceOf(RuntimeException.class);
        assertThat(events(a.mediaId())).isEqualTo(3); assertThat(events(b.mediaId())).isEqualTo(1);
    }
    @Test void storageKeysCompareCaseSensitivelyWithoutNormalizingLegacyLocations() {
        var a = asset(); String key = UUID.randomUUID()+"/Case.jpg";
        var first = catalog.registerVariant(variant(a.mediaId(), "MASTER", key), audit());
        var second = catalog.registerVariant(variant(a.mediaId(), "CASE_DISTINCT", key.replace("Case", "case")), audit());
        assertThat(catalog.findVariant(first.variantId()).orElseThrow().storageKey()).isEqualTo(key);
        assertThat(second.storageKey()).isNotEqualTo(first.storageKey());
    }
    @Test void referencesSupportMultipleCurrentConsumersAndPreserveHistory() {
        var a = asset();
        var one = catalog.recordReference(reference(a.mediaId(), "100"), audit());
        var two = catalog.recordReference(reference(a.mediaId(), "200"), audit());
        var historical = catalog.endReference(one.end(NOW.plusSeconds(1), null, null), audit());
        assertThat(historical.version()).isEqualTo(one.version()+1);
        assertThat(catalog.findReference(one.referenceId())).contains(historical);
        assertThat(catalog.findReference(two.referenceId()).orElseThrow().isCurrent()).isTrue();
        var replacement = catalog.recordReference(reference(a.mediaId(), "100"), audit());
        assertThat(replacement.referenceId()).isNotEqualTo(one.referenceId());
        assertThat(events(a.mediaId())).isEqualTo(5);
        assertThatThrownBy(() -> catalog.endReference(historical.end(NOW.plusSeconds(2), null, null), audit())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void databaseRejectsOrphansAndParentRemovalIncludingHistoricalReferences() {
        assertThatThrownBy(() -> catalog.registerVariant(variant(Long.MAX_VALUE, "MASTER", UUID.randomUUID().toString()), audit())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> catalog.recordReference(reference(Long.MAX_VALUE, "1"), audit())).isInstanceOf(RuntimeException.class);
        var a = asset(); var r = catalog.recordReference(reference(a.mediaId(), "1").end(NOW.plusSeconds(1), null, null), audit());
        assertThatThrownBy(() -> jdbc.update("delete from storage_media_assets where id=?", a.mediaId())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update("delete from storage_media_references where id=?", r.referenceId())).isInstanceOf(RuntimeException.class);
        assertThat(catalog.findReference(r.referenceId()).orElseThrow().isCurrent()).isFalse();
    }
    @Test void databaseChecksProtectMetadataEvenWhenDomainIsBypassed() {
        var a = asset(); var v = catalog.registerVariant(variant(a.mediaId(), "MASTER", UUID.randomUUID().toString()), audit());
        var r = catalog.recordReference(reference(a.mediaId(), "1"), audit());
        for (String assignment : new String[]{"size_bytes=-1", "width=1,height=null", "checksum_algorithm='SHA-256',checksum=null", "role='DERIVED'"}) {
            assertThatThrownBy(() -> jdbc.update("update storage_media_variants set "+assignment+" where id=?",v.variantId())).isInstanceOf(RuntimeException.class);
        }
        assertThatThrownBy(() -> jdbc.update("update storage_media_assets set subject_type='USER',subject_id=null where id=?", a.mediaId())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update("update storage_media_references set unlinked_by_user_id=1 where id=?", r.referenceId())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update("update storage_media_references set context='UNKNOWN' where id=?", r.referenceId())).isInstanceOf(RuntimeException.class);
    }
    @Test void auditTargetCannotBelongToAnotherAsset() {
        var a = asset(); var b = asset();
        var v = catalog.registerVariant(variant(a.mediaId(), "MASTER", UUID.randomUUID().toString()), audit());
        var r = catalog.recordReference(reference(a.mediaId(), "1"), audit());
        for (boolean variant : new boolean[]{true,false}) {
            assertThatThrownBy(() -> tx(() -> {
                em.persist(MediaCatalogMapper.toEntity(new MediaAuditEvent(null, b.mediaId(), variant?v.variantId():null,
                        variant?null:r.referenceId(), variant?MediaAuditOperation.VARIANT_REGISTERED:MediaAuditOperation.REFERENCE_RECORDED,
                        audit(), null, null))); em.flush(); return null;
            })).isInstanceOf(RuntimeException.class);
        }
    }
    @Test void rollbackIncludesAssetVariantReferencesAndAudit() {
        Long[] media = new Long[1];
        assertThatThrownBy(() -> tx(() -> {
            var a=asset(); media[0]=a.mediaId();
            catalog.registerVariant(variant(a.mediaId(),"MASTER",UUID.randomUUID().toString()),audit());
            catalog.recordReference(reference(a.mediaId(),"1"),audit());
            throw new IllegalStateException("synthetic rollback");
        })).hasMessage("synthetic rollback");
        assertThat(catalog.findAsset(media[0])).isEmpty(); assertThat(events(media[0])).isZero();
        assertThat(jdbc.queryForObject("select count(*) from storage_media_variants where media_id=?",Long.class,media[0])).isZero();
        assertThat(jdbc.queryForObject("select count(*) from storage_media_references where media_id=?",Long.class,media[0])).isZero();
    }
    @Test void metadataUpdatesCannotReassignProvenanceOrRewriteReferenceTargets() {
        var a = asset();
        var forged = new MediaAsset(a.mediaId(), a.origin(), a.sourceContext(), a.sourceRecordId(), 999L,
                a.ownerUserId(), a.subjectType(), a.subjectId(), "CHANGED", a.classification(), a.physicalState(),
                a.createdAt(), NOW.plusSeconds(1), a.version());
        assertThatThrownBy(() -> catalog.updateAssetMetadata(forged, audit())).isInstanceOf(IllegalArgumentException.class);
        var r = catalog.recordReference(reference(a.mediaId(), "1"), audit());
        var retargeted = new MediaReference(r.referenceId(), r.mediaId(), r.context(), "2", r.role(), r.position(),
                r.associationOrigin(), r.linkedAt(), r.linkedByUserId(), NOW.plusSeconds(1), null, null, r.version());
        assertThatThrownBy(() -> catalog.endReference(retargeted, audit())).isInstanceOf(IllegalArgumentException.class);
        assertThat(catalog.findAsset(a.mediaId())).contains(a);
        assertThat(catalog.findReference(r.referenceId())).contains(r);
        assertThat(events(a.mediaId())).isEqualTo(2);
    }
    @Test void auditInsertionFailureRollsBackTheBusinessRow() {
        var intercepted = org.mockito.Mockito.mock(EntityManager.class, org.mockito.AdditionalAnswers.delegatesTo(em));
        var inserted = new java.util.concurrent.atomic.AtomicReference<MediaAssetEntity>();
        org.mockito.Mockito.doAnswer(call -> {
            Object entity = call.getArgument(0);
            if (entity instanceof MediaAuditEventEntity) throw new jakarta.persistence.PersistenceException("synthetic audit failure");
            em.persist(entity);
            if (entity instanceof MediaAssetEntity asset) inserted.set(asset);
            return null;
        }).when(intercepted).persist(org.mockito.ArgumentMatchers.any());
        var failing = new MediaCatalogRepositoryAdapter(intercepted);
        assertThatThrownBy(() -> tx(() -> failing.registerAsset(MediaAsset.create(MediaOrigin.UNKNOWN, MediaContext.UNKNOWN,
                null, null, null, MediaSubjectType.UNKNOWN, null, "UNKNOWN", NOW), audit())))
                .isInstanceOf(jakarta.persistence.PersistenceException.class).hasMessage("synthetic audit failure");
        Long id = inserted.get().getMediaId();
        assertThat(id).isPositive(); assertThat(catalog.findAsset(id)).isEmpty(); assertThat(events(id)).isZero();
    }
    @Test void hibernateCanValidateTheCatalogSchema() {
        var registry = new org.hibernate.boot.registry.StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.datasource", jdbc.getDataSource())
                .applySetting("hibernate.hbm2ddl.auto", "validate").build();
        try (var ignored = new org.hibernate.boot.MetadataSources(registry)
                .addAnnotatedClass(MediaAssetEntity.class).addAnnotatedClass(MediaVariantEntity.class)
                .addAnnotatedClass(MediaReferenceEntity.class).addAnnotatedClass(MediaAuditEventEntity.class)
                .buildMetadata().buildSessionFactory()) {
            assertThat(ignored.isOpen()).isTrue();
        } finally { org.hibernate.boot.registry.StandardServiceRegistryBuilder.destroy(registry); }
    }
    @Test void staleDetachedMetadataCannotOverwriteNewerVersionOrAppendAudit() {
        var a=asset(); var changed=catalog.updateAssetMetadata(a.revisePurpose("REVIEWED_PURPOSE",NOW.plusSeconds(1)),audit());
        assertThat(changed.version()).isEqualTo(a.version()+1);
        assertThatThrownBy(() -> catalog.updateAssetMetadata(a.revisePurpose("STALE_PURPOSE",NOW.plusSeconds(2)),audit()))
                .isInstanceOfAny(jakarta.persistence.OptimisticLockException.class, org.springframework.orm.ObjectOptimisticLockingFailureException.class);
        assertThat(catalog.findAsset(a.mediaId())).contains(changed); assertThat(events(a.mediaId())).isEqualTo(2);
    }
    @Test void competingMetadataTransactionsCommitExactlyOneChangeAndOneEvent() throws Exception {
        var a=asset(); race(() -> {
            var current=catalog.findAsset(a.mediaId()).orElseThrow();
            return () -> catalog.updateAssetMetadata(current.revisePurpose("PURPOSE_"+Thread.currentThread().getId(),NOW.plusSeconds(1)),audit());
        });
        assertThat(catalog.findAsset(a.mediaId()).orElseThrow().version()).isEqualTo(1);
        assertThat(events(a.mediaId())).isEqualTo(2);
    }
    @Test void competingReferenceTransactionsKeepExactlyOneHistoricalEndAndEvent() throws Exception {
        var a=asset(); var r=catalog.recordReference(reference(a.mediaId(),"1"),audit());
        race(() -> {
            var current=catalog.findReference(r.referenceId()).orElseThrow();
            return () -> catalog.endReference(current.end(NOW.plusSeconds(1),null,null),audit());
        });
        assertThat(catalog.findReference(r.referenceId()).orElseThrow().version()).isEqualTo(1);
        assertThat(events(a.mediaId())).isEqualTo(3);
    }
    private void race(Supplier<Runnable> prepare) throws Exception {
        var ready=new CountDownLatch(2); var start=new CountDownLatch(1); var pool=Executors.newFixedThreadPool(2);
        Callable<Boolean> worker=() -> {
            try { return tx(() -> {
                Runnable write=prepare.get(); ready.countDown(); await(start); write.run(); return true;
            }); } catch (jakarta.persistence.OptimisticLockException | org.springframework.orm.ObjectOptimisticLockingFailureException expected) { return false; }
        };
        try {
            var one=pool.submit(worker); var two=pool.submit(worker);
            assertThat(ready.await(20,TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(java.util.List.of(one.get(30,TimeUnit.SECONDS),two.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        } finally { start.countDown(); pool.shutdownNow(); assertThat(pool.awaitTermination(10,TimeUnit.SECONDS)).isTrue(); }
    }
    private static void await(CountDownLatch latch) {
        try { if(!latch.await(20,TimeUnit.SECONDS))throw new IllegalStateException("barrier timeout"); }
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
    }
}
