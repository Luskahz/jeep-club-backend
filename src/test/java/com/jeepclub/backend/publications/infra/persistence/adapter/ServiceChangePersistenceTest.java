package com.jeepclub.backend.publications.infra.persistence.adapter;

import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.core.application.exception.ServiceOperationException;
import com.jeepclub.backend.publications.core.application.service.*;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationChangeRequestStatus;
import com.jeepclub.backend.publications.core.domain.enums.ServicePublicationRequestStatus;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
@ContextConfiguration(classes = ServiceChangePersistenceTest.JpaTestConfiguration.class)
@Import({PublicationRepositoryAdapter.class, PublicationLikeRepositoryAdapter.class,
        PublicationCommentRepositoryAdapter.class, ServicePublicationRequestRepositoryAdapter.class,
        ServicePublicationChangeRequestRepositoryAdapter.class, PublicationMapper.class, PublicationHistoryMapper.class,
        ServicePublicationRequestMapper.class, ServicePublicationChangeRequestMapper.class,
        AdminServicePublicationRequestService.class, ServicePublicationChangeRequestService.class,
        AdminServicePublicationChangeRequestService.class})
class ServiceChangePersistenceTest {
    private static final Instant NOW = Instant.parse("2026-09-27T12:00:00Z");
    private static final String KEY = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg";
    private static final String NEXT = "images/2026/09/27/550e8400-e29b-41d4-a716-446655440001.jpg";
    @Autowired private PublicationRepositoryAdapter publications;
    @Autowired private PublicationLikeRepositoryAdapter likes;
    @Autowired private PublicationLikeJpaRepository likeJpa;
    @Autowired private PublicationCommentRepositoryAdapter comments;
    @Autowired private ServicePublicationRequestRepositoryAdapter initialRequests;
    @MockitoSpyBean private ServicePublicationChangeRequestRepositoryAdapter changes;
    @Autowired private AdminServicePublicationRequestService adminInitial;
    @Autowired private ServicePublicationChangeRequestService ownerChanges;
    @Autowired private AdminServicePublicationChangeRequestService adminChanges;
    @Autowired private PublicationHistoryJpaRepository history;
    @Autowired private PublicationJpaRepository publicationJpa;
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager manager;
    @MockitoBean private ImageMediaService media;

    @Test void independentChangeRoundTripsPendingApprovedAndRejectedWithCompleteGallery() {
        long serviceId = serviceId();
        var pending = changes.save(ServicePublicationChangeRequest.create(serviceId, 7L, "Revised", "Next body",
                new BigDecimal("150"), "456", List.of(new PublicationImage(NEXT, 0, true)), NOW.plusSeconds(2)));
        em.clear();
        assertThat(changes.findById(pending.getId()).orElseThrow()).satisfies(item -> {
            assertThat(item.getStatus()).isEqualTo(ServicePublicationChangeRequestStatus.PENDING);
            assertThat(item.getProposedAmount()).isEqualByComparingTo("150.00");
            assertThat(item.getProposedImages()).containsExactly(new PublicationImage(NEXT, 0, true));
        });
        pending.approve(9L, NOW.plusSeconds(3));
        changes.save(pending);
        var rejected = changes.save(ServicePublicationChangeRequest.create(serviceId, 7L, "Other", "Body",
                new BigDecimal("200"), "789", List.of(new PublicationImage(KEY, 0, true)), NOW.plusSeconds(2)));
        rejected.reject(9L, " no ", NOW.plusSeconds(3));
        changes.save(rejected);
        em.flush(); em.clear();
        assertThat(changes.findById(pending.getId()).orElseThrow()).satisfies(item -> {
            assertThat(item.getStatus()).isEqualTo(ServicePublicationChangeRequestStatus.APPROVED);
            assertThat(item.getReviewedByUserId()).isEqualTo(9L);
            assertThat(item.getReviewedAt()).isEqualTo(NOW.plusSeconds(3));
            assertThat(item.getVersion()).isGreaterThanOrEqualTo(1L);
        });
        assertThat(changes.findById(rejected.getId()).orElseThrow()).satisfies(item -> {
            assertThat(item.getStatus()).isEqualTo(ServicePublicationChangeRequestStatus.REJECTED);
            assertThat(item.getRejectionReason()).isEqualTo("no");
        });
    }

    @Test void databaseUniquePendingKeyPreventsSecondPendingButAllowsLaterRequests() {
        long serviceId = serviceId();
        changes.save(change(serviceId));
        assertThatThrownBy(() -> changes.save(change(serviceId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void approvalUpdatesSameJoinedPublicationAndHistoryKeepsFinalSnapshotAndRequests() {
        var initial = initialRequests.save(initial());
        long serviceId = adminInitial.approve(initial.getId(), 9L).getCreatedPublicationId();
        var before = (ServicePublication) publications.findById(serviceId).orElseThrow();
        var like = likes.save(PublicationLike.create(serviceId, 11L, NOW.plusSeconds(2)));
        var comment = comments.save(PublicationComment.create(serviceId, 11L, "Interested",
                List.of(new PublicationCommentImage(KEY, 0)), NOW.plusSeconds(2)));
        var change = ownerChanges.create(serviceId, 7L, "Revised", "Next body", new BigDecimal("150"), "456",
                List.of(new PublicationImage(NEXT, 0, true)));
        assertThat(((ServicePublication) publications.findById(serviceId).orElseThrow()).getTitle()).isEqualTo("Original");
        adminChanges.approve(change.getId(), 9L);
        em.flush(); em.clear();
        var after = (ServicePublication) publications.findById(serviceId).orElseThrow();
        assertThat(after.getId()).isEqualTo(before.getId());
        assertThat(after.getSourceRequestId()).isEqualTo(initial.getId());
        assertThat(after.getPublishedAt()).isEqualTo(before.getPublishedAt());
        assertThat(after.getTitle()).isEqualTo("Revised");
        assertThat(after.getContent()).isEqualTo("Next body");
        assertThat(after.getAmount()).isEqualByComparingTo("150.00");
        assertThat(after.getContactPhone()).isEqualTo("456");
        assertThat(after.getImages()).containsExactly(new PublicationImage(NEXT, 0, true));
        assertThat(likeJpa.findById(like.id())).isPresent();
        assertThat(comments.findById(comment.getId())).isPresent();
        publications.delete(serviceId, 7L, NOW.plusSeconds(3));
        em.flush(); em.clear();
        assertThat(publicationJpa.findById(serviceId)).isEmpty();
        assertThat(initialRequests.findById(initial.getId())).isPresent();
        assertThat(changes.findById(change.getId())).isPresent();
        var snapshot = history.findAll().stream().filter(item -> item.getPublicationId().equals(serviceId))
                .map(ServicePublicationHistoryEntity.class::cast).findFirst().orElseThrow();
        assertThat(snapshot.getTitle()).isEqualTo("Revised");
        assertThat(snapshot.getAmount()).isEqualByComparingTo("150.00");
        assertThat(snapshot.getContactPhone()).isEqualTo("456");
        assertThat(snapshot.getSourceRequestId()).isEqualTo(initial.getId());
        assertThat(snapshot.getImages()).singleElement().satisfies(image -> assertThat(image.getStorageKey()).isEqualTo(NEXT));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void twoConcurrentOwnersCreateAtMostOnePendingChange() throws Exception {
        var tx = new TransactionTemplate(manager);
        Long serviceId = tx.execute(status -> serviceId());
        var firstSaved = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var secondEntered = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> tx.execute(status -> {
                ownerChanges.create(serviceId, 7L, "First", null, null, null, null);
                firstSaved.countDown();
                await(release);
                return null;
            }));
            assertThat(firstSaved.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> {
                secondEntered.countDown();
                return ownerChanges.create(serviceId, 7L, "Second", null, null, null, null);
            });
            assertThat(secondEntered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(ServiceOperationException.class);
            Long pendingCount = tx.execute(status -> changes.findAll(ServicePublicationChangeRequestStatus.PENDING,
                    org.springframework.data.domain.Pageable.unpaged()).stream()
                    .filter(item -> item.getServicePublicationId().equals(serviceId)).count());
            assertThat(pendingCount).isEqualTo(1L);
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void twoConcurrentApprovalsApplyOneChangeOnce() throws Exception {
        var tx = new TransactionTemplate(manager);
        Long changeId = tx.execute(status -> {
            long serviceId = serviceId();
            return changes.save(change(serviceId)).getId();
        });
        var firstApproved = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var secondEntered = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> tx.execute(status -> {
                adminChanges.approve(changeId, 9L);
                firstApproved.countDown();
                await(release);
                return null;
            }));
            assertThat(firstApproved.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> {
                secondEntered.countDown();
                return adminChanges.approve(changeId, 10L);
            });
            assertThat(secondEntered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(ServiceOperationException.class);
            Long reviewer = tx.execute(status -> changes.findById(changeId).orElseThrow().getReviewedByUserId());
            assertThat(reviewer).isEqualTo(9L);
        } finally { release.countDown(); executor.shutdownNow(); }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedChangeReviewSaveRollsBackServiceUpdateAndDeletedTargetKeepsRequestPending() {
        var tx = new TransactionTemplate(manager);
        Long[] ids = tx.execute(status -> {
            long serviceId = serviceId();
            return new Long[]{serviceId, changes.save(change(serviceId)).getId()};
        });
        doThrow(new IllegalStateException("review save failed")).when(changes)
                .save(argThat(item -> item.getStatus() == ServicePublicationChangeRequestStatus.APPROVED));
        assertThatThrownBy(() -> adminChanges.approve(ids[1], 9L)).isInstanceOf(IllegalStateException.class);
        String title = tx.execute(status -> ((ServicePublication) publications.findById(ids[0]).orElseThrow()).getTitle());
        assertThat(title).isEqualTo("Original");
        ServicePublicationChangeRequestStatus pending = tx.execute(status -> changes.findById(ids[1]).orElseThrow().getStatus());
        assertThat(pending).isEqualTo(ServicePublicationChangeRequestStatus.PENDING);
        tx.executeWithoutResult(status -> publications.delete(ids[0], 9L, NOW.plusSeconds(3)));
        assertThatThrownBy(() -> adminChanges.approve(ids[1], 9L)).isInstanceOf(ServiceOperationException.class);
        ServicePublicationChangeRequestStatus afterDelete = tx.execute(status -> changes.findById(ids[1]).orElseThrow().getStatus());
        assertThat(afterDelete).isEqualTo(ServicePublicationChangeRequestStatus.PENDING);
    }

    private long serviceId() {
        var request = initialRequests.save(initial());
        return adminInitial.approve(request.getId(), 9L).getCreatedPublicationId();
    }
    private static ServicePublicationRequest initial() {
        return ServicePublicationRequest.create(7L, "Original", "Body", new BigDecimal("100.00"), "123",
                List.of(new PublicationImage(KEY, 0, true)), NOW);
    }
    private static ServicePublicationChangeRequest change(long serviceId) {
        return ServicePublicationChangeRequest.create(serviceId, 7L, "Revised", "Next body", new BigDecimal("150.00"),
                "456", List.of(new PublicationImage(NEXT, 0, true)), NOW.plusSeconds(2));
    }
    private static void await(CountDownLatch latch) {
        try { if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Release timed out"); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new AssertionError(exception); }
    }

    @TestConfiguration @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = PublicationJpaRepository.class)
    @EntityScan(basePackageClasses = PublicationEntity.class)
    static class JpaTestConfiguration {
        @Bean Clock clock() { return Clock.fixed(NOW.plusSeconds(2), ZoneOffset.UTC); }
    }
}
