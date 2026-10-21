package com.jeepclub.backend.billing.infra.persistence.jpa;
import com.jeepclub.backend.billing.infra.persistence.entity.ChargeDefinitionDeletionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ChargeDefinitionDeletionHistoryJpaRepository extends JpaRepository<ChargeDefinitionDeletionHistoryEntity, Long> {}
