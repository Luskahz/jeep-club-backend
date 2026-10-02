package com.jeepclub.backend.identity.infra.persistence;

import com.jeepclub.backend.iam.identity.core.application.service.user.UserProfileService;
import com.jeepclub.backend.iam.identity.core.domain.model.ResidentialAddress;
import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.core.domain.model.UserProfile;
import com.jeepclub.backend.iam.identity.core.domain.model.WorkProfile;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import com.jeepclub.backend.iam.identity.infra.persistence.jpa.UserJpaRepository;
import com.jeepclub.backend.iam.identity.infra.persistence.jpa.UserProfileJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.jeepclub.backend.BackendApplication.class,
        properties = "spring.datasource.url=jdbc:h2:mem:identity_profile_concurrency;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=USER")
@ActiveProfiles("test")
class UserProfileConcurrencyIntegrationTest {
    @Autowired UserProfileService service;
    @Autowired UserRepository users;
    @Autowired UserJpaRepository userJpa;
    @Autowired UserProfileJpaRepository profileJpa;
    @Autowired TransactionTemplate transactions;

    @Test
    void firstConcurrentUpsertsWaitForParentLockAndPersistOneWholeReplacement() throws Exception {
        Long userId = transactions.execute(status -> users.create(User.create(
                "Concurrent user", null, null, "52998224725", null, null, null,
                Instant.parse("2026-10-02T12:00:00Z"))).getId());
        var firstProfile = new UserProfile(userId, new WorkProfile("Mechanic", "Garage"),
                new ResidentialAddress("12345-678", "First street", "1", null, "Center", "City", "SP"));
        var secondProfile = new UserProfile(userId, new WorkProfile("Driver", "Workshop"),
                new ResidentialAddress("87654321", "Second street", "2", null, "District", "Town", "RJ"));
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            assertThat(service.get(userId).profileCompletionPending()).isTrue();
            assertThat(profileJpa.existsById(userId)).isFalse();
            var first = executor.submit(() -> transactions.execute(status -> {
                // Hold the same parent lock before any profile row exists.
                users.findByIdForUpdate(userId).orElseThrow();
                locked.countDown();
                await(release);
                return service.replace(userId, firstProfile.workProfile(), firstProfile.address());
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> {
                secondStarted.countDown();
                return service.replace(userId, secondProfile.workProfile(), secondProfile.address());
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            assertThat(profileJpa.existsById(userId)).isFalse();
            release.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo(firstProfile);
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo(secondProfile);
            assertThat(service.get(userId)).isEqualTo(secondProfile);
            assertThat(profileJpa.findAll()).hasSize(1);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
            transactions.executeWithoutResult(status -> {
                profileJpa.deleteById(userId);
                userJpa.deleteById(userId);
            });
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for test transaction");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test transaction interrupted", exception);
        }
    }
}
