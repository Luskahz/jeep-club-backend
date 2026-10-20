package com.jeepclub.backend.iam.identity.infra.persistence.jpa;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserDeletionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserDeletionHistoryJpaRepository extends JpaRepository<UserDeletionHistoryEntity, Long> {}
