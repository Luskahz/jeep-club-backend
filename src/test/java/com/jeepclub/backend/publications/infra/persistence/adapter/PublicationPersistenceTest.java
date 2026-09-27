package com.jeepclub.backend.publications.infra.persistence.adapter;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.exception.PublicationAlreadyDeletedException;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.infra.persistence.entity.*;
import com.jeepclub.backend.publications.infra.persistence.jpa.*;
import com.jeepclub.backend.publications.infra.persistence.mapper.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PublicationPersistenceTest.JpaTestConfiguration.class)
@Import({PublicationRepositoryAdapter.class, PublicationLikeRepositoryAdapter.class,
        PublicationCommentRepositoryAdapter.class, PublicationMapper.class, PublicationHistoryMapper.class})
class PublicationPersistenceTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final String OTHER_KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440001.webp";

    @Autowired private PublicationRepositoryAdapter repository;
    @Autowired private PublicationLikeRepositoryAdapter likeRepository;
    @Autowired private PublicationCommentRepositoryAdapter commentRepository;
    @Autowired private PublicationJpaRepository jpa;
    @Autowired private PublicationHistoryJpaRepository history;
    @Autowired private PublicationLikeJpaRepository likes;
    @Autowired private PublicationCommentJpaRepository comments;
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test void joinedInheritancePersistsAndReconstructsAllConcreteSubtypes() {
        Publication notice = repository.save(Notice.create(7L, "Notice", "Body", gallery(), NOW));
        Publication event = repository.save(Event.create(7L, "Event", "Body", gallery(), NOW.plusSeconds(3600), NOW));
        Publication service = repository.save(ServicePublication.create(8L, "Service", "Body", gallery(), NOW));
        em.clear();

        assertThat(jpa.findById(notice.getId()).orElseThrow()).isInstanceOf(NoticeEntity.class);
        assertThat(jpa.findById(event.getId()).orElseThrow()).isInstanceOf(EventEntity.class);
        assertThat(jpa.findById(service.getId()).orElseThrow()).isInstanceOf(ServicePublicationEntity.class);
        assertThat(repository.findById(notice.getId()).orElseThrow()).isInstanceOf(Notice.class);
        assertThat(repository.findById(event.getId()).orElseThrow()).isInstanceOf(Event.class)
                .satisfies(p -> assertThat(((Event) p).getStartsAt()).isEqualTo(NOW.plusSeconds(3600)));
        assertThat(repository.findById(service.getId()).orElseThrow()).isInstanceOf(ServicePublication.class);
        assertThat(repository.findById(notice.getId()).orElseThrow().getImages())
                .extracting(PublicationImage::storageKey).containsExactly(KEY, OTHER_KEY);
        assertThat(repository.findById(notice.getId()).orElseThrow().getImages())
                .extracting(PublicationImage::primary).containsExactly(true, false);
    }

    @Test void lifecycleSaveUpdatesExistingJoinedRowAndImageOrder() {
        Publication saved = repository.save(Notice.create(7L, "Notice", "Body", gallery(), NOW));
        saved.publish(NOW.plusSeconds(10));
        saved.replaceImages(List.of(new PublicationImage(OTHER_KEY, 0, true)), NOW.plusSeconds(11));
        repository.save(saved);
        em.flush();
        em.clear();
        Publication restored = repository.findById(saved.getId()).orElseThrow();
        assertThat(restored.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(restored.getPublishedAt()).isEqualTo(NOW.plusSeconds(10));
        assertThat(restored.getImages()).containsExactly(new PublicationImage(OTHER_KEY, 0, true));
    }

    @Test void likesHaveDatabaseUniquenessAndCommentsKeepImages() {
        Publication saved = repository.save(Notice.create(7L, "Notice", "Body", gallery(), NOW));
        PublicationLike like = likeRepository.save(PublicationLike.create(saved.getId(), 9L, NOW));
        assertThat(like.id()).isNotNull();
        assertThatThrownBy(() -> likeRepository.save(PublicationLike.create(saved.getId(), 9L, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void commentsRoundTripWithGlobalImageKeys() {
        Publication saved = repository.save(Notice.create(7L, "Notice", "Body", gallery(), NOW));
        PublicationComment comment = commentRepository.save(PublicationComment.create(saved.getId(), 9L, "Hello",
                List.of(new PublicationCommentImage(KEY, 0)), NOW));
        assertThat(comment.getId()).isNotNull();
        em.flush();
        em.clear();
        PublicationCommentEntity entity = comments.findById(comment.getId()).orElseThrow();
        assertThat(entity.getPublication().getId()).isEqualTo(saved.getId());
        assertThat(entity.getImages()).singleElement().satisfies(image -> {
            assertThat(image.getStorageKey()).isEqualTo(KEY);
            assertThat(image.getPosition()).isZero();
        });
        PublicationComment restored = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(restored.getContent()).isEqualTo("Hello");
        assertThat(restored.getImages()).containsExactly(new PublicationCommentImage(KEY, 0));
    }

    @Test void deleteSnapshotsSubtypeImagesActorAndTimeThenRejectsRepeat() {
        Publication saved = repository.save(Event.create(7L, "Event", "Body", gallery(), NOW.plusSeconds(3600), NOW));
        likeRepository.save(PublicationLike.create(saved.getId(), 9L, NOW));
        commentRepository.save(PublicationComment.create(saved.getId(), 9L, "Comment",
                List.of(new PublicationCommentImage(KEY, 0)), NOW));
        repository.delete(saved.getId(), 11L, NOW.plusSeconds(60));
        em.flush();
        em.clear();
        assertThat(jpa.findById(saved.getId())).isEmpty();
        assertThat(likes.findAll()).isEmpty();
        assertThat(comments.findAll()).isEmpty();
        assertThat(history.findAll()).singleElement().isInstanceOf(EventHistoryEntity.class)
                .satisfies(snapshot -> {
                    assertThat(snapshot.getPublicationId()).isEqualTo(saved.getId());
                    assertThat(snapshot.getAuthorUserId()).isEqualTo(7L);
                    assertThat(snapshot.getTitle()).isEqualTo("Event");
                    assertThat(snapshot.getContent()).isEqualTo("Body");
                    assertThat(snapshot.getStatus()).isEqualTo(PublicationStatus.DRAFT);
                    assertThat(snapshot.getCreatedAt()).isEqualTo(NOW);
                    assertThat(snapshot.getDeletedByUserId()).isEqualTo(11L);
                    assertThat(snapshot.getDeletedAt()).isEqualTo(NOW.plusSeconds(60));
                    assertThat(((EventHistoryEntity) snapshot).getStartsAt()).isEqualTo(NOW.plusSeconds(3600));
                    assertThat(snapshot.getImages()).extracting(PublicationImageEntity::getStorageKey)
                            .containsExactly(KEY, OTHER_KEY);
                    assertThat(snapshot.getImages()).extracting(PublicationImageEntity::isPrimary)
                            .containsExactly(true, false);
                });
        assertThatThrownBy(() -> repository.delete(saved.getId(), 11L, NOW.plusSeconds(61)))
                .isInstanceOf(PublicationAlreadyDeletedException.class);
    }

    @Test void noticeAndServiceHistoryKeepConcreteSubtype() {
        Publication notice = repository.save(Notice.create(7L, "Notice", "Body", gallery(), NOW));
        Publication service = repository.save(ServicePublication.create(7L, "Service", "Body", gallery(), NOW));
        repository.delete(notice.getId(), 11L, NOW.plusSeconds(1));
        repository.delete(service.getId(), 11L, NOW.plusSeconds(1));
        em.flush();
        em.clear();
        assertThat(history.findAll()).hasSize(2)
                .anyMatch(NoticeHistoryEntity.class::isInstance)
                .anyMatch(ServicePublicationHistoryEntity.class::isInstance);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void historyFailureRollsBackOperationalDelete() {
        var transactions = new TransactionTemplate(transactionManager);
        Long id = transactions.execute(status -> repository.save(Notice.create(7L, "Notice", "Body", gallery(), NOW)).getId());
        assertThat(id).isNotNull();
        // A duplicate snapshot deterministically fails the unique original-id constraint.
        assertThatThrownBy(() -> transactions.execute(status -> {
            PublicationEntity entity = jpa.findById(id).orElseThrow();
            var duplicate = new PublicationHistoryMapper().snapshot(entity, 11L, NOW.plusSeconds(1));
            history.saveAndFlush(duplicate);
            repository.delete(id, 11L, NOW.plusSeconds(2));
            return null;
        })).isInstanceOf(DataIntegrityViolationException.class);
        java.util.Optional<PublicationEntity> stillPresent = transactions.execute(status -> jpa.findById(id));
        List<PublicationHistoryEntity> snapshots = transactions.execute(status -> history.findAll());
        assertThat(stillPresent).isPresent();
        assertThat(snapshots).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentDeleteWaitsForLockAndOnlyOneSnapshotSurvives() throws Exception {
        var transactions = new TransactionTemplate(transactionManager);
        Long id = transactions.execute(status -> repository.save(Notice.create(7L, "Notice", "Body", gallery(), NOW)).getId());
        var firstDeleted = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var secondEntered = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> transactions.execute(status -> {
                repository.delete(id, 11L, NOW.plusSeconds(1));
                firstDeleted.countDown();
                try {
                    if (!releaseFirst.await(5, TimeUnit.SECONDS)) throw new AssertionError("Release timed out");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                return null;
            }));
            assertThat(firstDeleted.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> transactions.execute(status -> {
                secondEntered.countDown();
                repository.delete(id, 12L, NOW.plusSeconds(2));
                return null;
            }));
            assertThat(secondEntered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(PublicationAlreadyDeletedException.class);
            Long snapshotCount = transactions.execute(status -> history.count());
            assertThat(snapshotCount).isEqualTo(1L);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    private static List<PublicationImage> gallery() {
        return List.of(new PublicationImage(KEY, 0, true), new PublicationImage(OTHER_KEY, 1, false));
    }

    @TestConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = PublicationJpaRepository.class)
    @EntityScan(basePackageClasses = PublicationEntity.class)
    static class JpaTestConfiguration { }
}
