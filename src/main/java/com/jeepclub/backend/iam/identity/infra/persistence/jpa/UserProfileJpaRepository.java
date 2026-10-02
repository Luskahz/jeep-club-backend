package com.jeepclub.backend.iam.identity.infra.persistence.jpa;

import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, Long> {}
