package com.jeepclub.backend.publications.infra.integration.lifecycle;
import com.jeepclub.backend.iam.identity.api.module.spi.UserDeletionGuard;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
@Component @RequiredArgsConstructor
public class PublicationsUserDeletionGuard implements UserDeletionGuard {
    private final EntityManager entityManager;
    @Override @Transactional(propagation = Propagation.MANDATORY)
    public boolean hasRetainedReferences(Long userId) {
        return java.util.List.of("SELECT id FROM event_guest_transitions WHERE actor_user_id = :id LIMIT 1 FOR UPDATE","SELECT id FROM event_guest_requests WHERE (requester_user_id = :id OR reviewer_id = :id OR responsible_user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM event_registrations WHERE (user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM event_ride_offers WHERE (user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM publication_comments WHERE (author_user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM publication_history WHERE (author_user_id = :id OR deleted_by_user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM publication_likes WHERE (member_user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM publications WHERE (author_user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM service_publication_change_requests WHERE (requested_by_user_id = :id OR reviewed_by_user_id = :id) LIMIT 1 FOR UPDATE","SELECT id FROM service_publication_requests WHERE (requested_by_user_id = :id OR reviewed_by_user_id = :id) LIMIT 1 FOR UPDATE").stream().anyMatch(sql ->
                !entityManager.createNativeQuery(sql).setParameter("id",userId).getResultList().isEmpty());
    }
}
