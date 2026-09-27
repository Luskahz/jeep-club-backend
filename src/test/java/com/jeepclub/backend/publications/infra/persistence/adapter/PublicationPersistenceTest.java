package com.jeepclub.backend.publications.infra.persistence.adapter;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
import com.jeepclub.backend.publications.core.application.service.AdminServicePublicationRequestService;
import com.jeepclub.backend.publications.core.application.service.PublicationLikeService;
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
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PublicationPersistenceTest.JpaTestConfiguration.class)
@Import({PublicationRepositoryAdapter.class, PublicationLikeRepositoryAdapter.class,
        PublicationCommentRepositoryAdapter.class, PublicationMapper.class, PublicationHistoryMapper.class,
        ServicePublicationRequestRepositoryAdapter.class, ServicePublicationRequestMapper.class,
        AdminServicePublicationRequestService.class, PublicationLikeService.class})
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
    @MockitoSpyBean private ServicePublicationRequestRepositoryAdapter requestRepository;
    @Autowired private ServicePublicationRequestJpaRepository requestJpa;
    @Autowired private AdminServicePublicationRequestService adminRequests;
    @Autowired private PublicationLikeService socialLikes;

    @Test void socialCountsAndCommentPageUseDatabaseQueries() {
        var first = repository.save(Notice.create(7L, "First", "Body", gallery(), NOW));
        var second = repository.save(Event.create(7L, "Second", "Body", gallery(), NOW.plusSeconds(3600), NOW));
        likeRepository.save(PublicationLike.create(first.getId(), 9L, NOW));
        likeRepository.save(PublicationLike.create(first.getId(), 10L, NOW));
        likeRepository.save(PublicationLike.create(second.getId(), 9L, NOW));
        commentRepository.save(PublicationComment.create(first.getId(), 9L, "One", List.of(), NOW));
        commentRepository.save(PublicationComment.create(first.getId(), 10L, null,
                List.of(new PublicationCommentImage(KEY, 0)), NOW));
        var ids = List.of(first.getId(), second.getId());
        assertThat(likeRepository.counts(ids)).containsEntry(first.getId(), 2L).containsEntry(second.getId(), 1L);
        assertThat(commentRepository.counts(ids)).containsEntry(first.getId(), 2L);
        assertThat(likeRepository.likedByMember(ids, 9L)).containsExactlyInAnyOrderElementsOf(ids);
        var page = commentRepository.findByPublication(first.getId(), org.springframework.data.domain.PageRequest.of(0, 1,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("createdAt"),
                        org.springframework.data.domain.Sort.Order.desc("id"))));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).singleElement().satisfies(c -> assertThat(c.getImages()).hasSize(1));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentLikeForSameMemberSerializesAndPersistsOneRow() throws Exception {
        var transactions = new TransactionTemplate(transactionManager);
        Long id = transactions.execute(status -> {
            var notice = Notice.create(7L, "Concurrent like", "Body", gallery(), NOW);
            notice.publish(NOW.plusSeconds(1));
            return repository.save(notice).getId();
        });
        var firstLiked = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var secondEntered = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> transactions.execute(status -> {
                socialLikes.like(id, 9L);
                firstLiked.countDown();
                try {
                    if (!releaseFirst.await(5, TimeUnit.SECONDS)) throw new AssertionError("Release timed out");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                return null;
            }));
            assertThat(firstLiked.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> {
                secondEntered.countDown();
                return socialLikes.like(id, 9L);
            });
            assertThat(secondEntered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            List<PublicationLikeEntity> persisted = transactions.execute(status -> likes.findAllByPublication_Id(id));
            assertThat(persisted).hasSize(1);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            transactions.execute(status -> {
                likes.deleteAll(likes.findAllByPublication_Id(id));
                likes.flush();
                jpa.deleteById(id);
                jpa.flush();
                return null;
            });
        }
    }

    @Test void publishedFeedFiltersConcreteTypesAndExcludesDraftAndArchived() {
        var publishedBase = Instant.parse("2050-01-01T00:00:00Z");
        var notice = repository.save(Notice.create(7L, "Notice", "Body", gallery(), NOW));
        notice.publish(publishedBase.plusSeconds(1));
        repository.save(notice);
        var event = repository.save(Event.create(7L, "Event", "Body", gallery(), NOW.plusSeconds(100), NOW));
        event.publish(publishedBase.plusSeconds(2));
        repository.save(event);
        repository.save(Notice.create(7L, "Draft", "Body", gallery(), NOW));
        var archived = repository.save(Notice.create(7L, "Archived", "Body", gallery(), NOW));
        archived.publish(publishedBase.plusSeconds(3));
        archived.archive(publishedBase.plusSeconds(4));
        repository.save(archived);
        em.flush();
        em.clear();
        var page = repository.findPublished(null, publishedBase, publishedBase.plusSeconds(10),
                org.springframework.data.domain.PageRequest.of(0, 10,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("publishedAt"),
                                org.springframework.data.domain.Sort.Order.desc("id"))));
        assertThat(page.getContent()).extracting(Publication::getId).containsExactly(event.getId(), notice.getId());
        assertThat(repository.findPublished("EVENT", publishedBase, publishedBase.plusSeconds(10), org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent()).extracting(Publication::getId).containsExactly(event.getId());
    }

    @Test void feedShowsApprovedServiceButNotItsRequest() {
        var pending = requestRepository.save(request(7L));
        long requestId = pending.getId();
        var before = repository.findPublished("SERVICE", NOW, NOW.plusSeconds(10),
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(before.getContent()).noneMatch(p -> p instanceof ServicePublication service
                && service.getSourceRequestId().equals(requestId));
        var materialized = repository.save(ServicePublication.fromApprovedRequest(pending, NOW.plusSeconds(1)));
        var after = repository.findPublished("SERVICE", NOW, NOW.plusSeconds(10),
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(after.getContent()).anyMatch(p -> p.getId().equals(materialized.getId()));
        assertThat(after.getContent()).allMatch(ServicePublication.class::isInstance);
    }

    @Test void joinedInheritancePersistsAndReconstructsAllConcreteSubtypes() {
        Publication notice = repository.save(Notice.create(7L, "Notice", "Body", gallery(), NOW));
        Publication event = repository.save(Event.create(7L, "Event", "Body", gallery(), NOW.plusSeconds(3600), NOW));
        Publication service = repository.save(service());
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

    @Test void editedNoticeContentPersistsAndIsPreservedByHardDeleteHistory() {
        Notice notice = (Notice) repository.save(Notice.create(7L, "Original", "Body", gallery(), NOW));
        notice.updateContent(" Revised ", " Revised body ", NOW.plusSeconds(1));
        repository.save(notice);
        em.clear();
        Notice restored = (Notice) repository.findById(notice.getId()).orElseThrow();
        assertThat(restored.getTitle()).isEqualTo("Revised");
        assertThat(restored.getContent()).isEqualTo("Revised body");
        assertThat(restored.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
        repository.delete(notice.getId(), 11L, NOW.plusSeconds(2));
        em.flush();
        em.clear();
        assertThat(repository.findById(notice.getId())).isEmpty();
        var snapshot = history.findAll().stream().filter(item -> item.getPublicationId().equals(notice.getId()))
                .findFirst().orElseThrow();
        assertThat(snapshot).isInstanceOf(NoticeHistoryEntity.class);
        assertThat(snapshot.getTitle()).isEqualTo("Revised");
        assertThat(snapshot.getContent()).isEqualTo("Revised body");
        assertThat(snapshot.getDeletedByUserId()).isEqualTo(11L);
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
        Publication service = repository.save(service());
        repository.delete(notice.getId(), 11L, NOW.plusSeconds(1));
        repository.delete(service.getId(), 11L, NOW.plusSeconds(1));
        em.flush();
        em.clear();
        assertThat(history.findAll()).hasSize(2)
                .anyMatch(NoticeHistoryEntity.class::isInstance)
                .anyMatch(ServicePublicationHistoryEntity.class::isInstance);
        ServicePublicationHistoryEntity snapshot = history.findAll().stream()
                .filter(ServicePublicationHistoryEntity.class::isInstance)
                .map(ServicePublicationHistoryEntity.class::cast).findFirst().orElseThrow();
        assertThat(snapshot.getSourceRequestId()).isEqualTo(999L);
        assertThat(snapshot.getAmount()).isEqualByComparingTo("12.00");
        assertThat(snapshot.getContactPhone()).isEqualTo("123456789");
    }

    @Test void independentRequestRoundTripsPendingApprovedAndRejectedWithImages() {
        long publicationsBefore = jpa.count();
        var pending = requestRepository.save(request(7L));
        var rejected = requestRepository.save(request(8L));
        em.clear();
        assertThat(requestRepository.findById(pending.getId()).orElseThrow())
                .satisfies(r -> {
                    assertThat(r.getStatus()).isEqualTo(ServicePublicationRequestStatus.PENDING);
                    assertThat(r.getImages()).extracting(PublicationImage::primary).containsExactly(true, false);
                    assertThat(r.getRequestedAt()).isEqualTo(NOW);
                });
        pending.approve(9L, 41L, NOW.plusSeconds(1));
        rejected.reject(9L, "  unsuitable  ", NOW.plusSeconds(1));
        requestRepository.save(pending);
        requestRepository.save(rejected);
        em.flush();
        em.clear();
        assertThat(requestRepository.findById(pending.getId()).orElseThrow())
                .satisfies(r -> {
                    assertThat(r.getStatus()).isEqualTo(ServicePublicationRequestStatus.APPROVED);
                    assertThat(r.getCreatedPublicationId()).isEqualTo(41L);
                    assertThat(r.getReviewedByUserId()).isEqualTo(9L);
                    assertThat(r.getReviewedAt()).isEqualTo(NOW.plusSeconds(1));
                    assertThat(r.getImages()).extracting(PublicationImage::storageKey).containsExactly(KEY, OTHER_KEY);
                    assertThat(r.getVersion()).isGreaterThanOrEqualTo(1L);
                });
        assertThat(requestRepository.findById(rejected.getId()).orElseThrow())
                .satisfies(r -> {
                    assertThat(r.getStatus()).isEqualTo(ServicePublicationRequestStatus.REJECTED);
                    assertThat(r.getRejectionReason()).isEqualTo("unsuitable");
                    assertThat(r.getCreatedPublicationId()).isNull();
                });
        assertThat(jpa.count()).isEqualTo(publicationsBefore);
    }

    @Test void approvedServiceKeepsRequestAfterHardDelete() {
        var pending = requestRepository.save(request(7L));
        var approved = adminRequests.approve(pending.getId(), 9L);
        var published = (ServicePublication) repository.findById(approved.getCreatedPublicationId()).orElseThrow();
        assertThat(published.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(published.getPublishedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(published.getSourceRequestId()).isEqualTo(pending.getId());
        assertThat(published.getAmount()).isEqualByComparingTo("12.00");
        assertThat(published.getContactPhone()).isEqualTo("123456789");
        repository.delete(published.getId(), 11L, NOW.plusSeconds(2));
        em.flush();
        em.clear();
        assertThat(requestRepository.findById(pending.getId()).orElseThrow().getStatus())
                .isEqualTo(ServicePublicationRequestStatus.APPROVED);
        var snapshot = history.findAll().stream().filter(item -> item.getPublicationId().equals(published.getId()))
                .map(ServicePublicationHistoryEntity.class::cast).findFirst().orElseThrow();
        assertThat(snapshot.getSourceRequestId()).isEqualTo(pending.getId());
        assertThat(snapshot.getAmount()).isEqualByComparingTo("12.00");
        assertThat(snapshot.getContactPhone()).isEqualTo("123456789");
        assertThat(snapshot.getImages()).hasSize(2);
    }

    @Test void rejectingRequestPersistsReviewWithoutCreatingService() {
        long publicationsBefore = jpa.count();
        var pending = requestRepository.save(request(7L));
        var rejected = adminRequests.reject(pending.getId(), 9L, "  insufficient detail  ");
        em.flush();
        em.clear();
        assertThat(requestRepository.findById(rejected.getId()).orElseThrow())
                .satisfies(r -> {
                    assertThat(r.getStatus()).isEqualTo(ServicePublicationRequestStatus.REJECTED);
                    assertThat(r.getReviewedByUserId()).isEqualTo(9L);
                    assertThat(r.getReviewedAt()).isEqualTo(NOW.plusSeconds(1));
                    assertThat(r.getRejectionReason()).isEqualTo("insufficient detail");
                    assertThat(r.getCreatedPublicationId()).isNull();
                });
        assertThat(jpa.count()).isEqualTo(publicationsBefore);
    }

    @Test void databasePreventsTwoServicesFromOneRequest() {
        var pending = requestRepository.save(request(7L));
        repository.save(ServicePublication.fromApprovedRequest(pending, NOW.plusSeconds(1)));
        assertThatThrownBy(() -> repository.save(ServicePublication.fromApprovedRequest(pending, NOW.plusSeconds(1))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedApprovalSaveRollsBackCreatedService() {
        var transactions = new TransactionTemplate(transactionManager);
        Long id = transactions.execute(status -> requestRepository.save(request(7L)).getId());
        Long countBefore = transactions.execute(status -> jpa.count());
        doThrow(new IllegalStateException("request save failed")).when(requestRepository)
                .save(argThat(r -> r.getStatus() == ServicePublicationRequestStatus.APPROVED));
        assertThatThrownBy(() -> adminRequests.approve(id, 9L)).isInstanceOf(IllegalStateException.class);
        Long serviceCount = transactions.execute(status -> jpa.count());
        ServicePublicationRequestStatus state = transactions.execute(status -> requestRepository.findById(id).orElseThrow().getStatus());
        assertThat(serviceCount).isEqualTo(countBefore);
        assertThat(state).isEqualTo(ServicePublicationRequestStatus.PENDING);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentApprovalWaitsForRequestLockAndCreatesOnlyOneService() throws Exception {
        var transactions = new TransactionTemplate(transactionManager);
        Long id = transactions.execute(status -> requestRepository.save(request(7L)).getId());
        Long countBefore = transactions.execute(status -> jpa.count());
        var approved = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var secondEntered = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> transactions.execute(status -> {
                adminRequests.approve(id, 9L);
                approved.countDown();
                try {
                    if (!releaseFirst.await(5, TimeUnit.SECONDS)) throw new AssertionError("Release timed out");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                return null;
            }));
            assertThat(approved.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> {
                secondEntered.countDown();
                return adminRequests.approve(id, 10L);
            });
            assertThat(secondEntered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(IllegalStateException.class);
            Long serviceCount = transactions.execute(status -> jpa.count());
            Long createdId = transactions.execute(status -> requestRepository.findById(id).orElseThrow().getCreatedPublicationId());
            assertThat(serviceCount).isEqualTo(countBefore + 1);
            assertThat(createdId).isNotNull();
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
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

    private static ServicePublication service() {
        var request = ServicePublicationRequest.reconstitute(999L, 7L, "Service", "Body", new BigDecimal("12.00"),
                "123456789", gallery(), ServicePublicationRequestStatus.PENDING, null, null, null,
                NOW, null, NOW, 0L);
        return ServicePublication.fromApprovedRequest(request, NOW);
    }

    private static ServicePublicationRequest request(Long requester) {
        return ServicePublicationRequest.create(requester, "Service", "Body", new BigDecimal("12.00"),
                "123456789", gallery(), NOW);
    }

    @TestConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = PublicationJpaRepository.class)
    @EntityScan(basePackageClasses = PublicationEntity.class)
    static class JpaTestConfiguration {
        @Bean Clock clock() { return Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC); }
    }
}
