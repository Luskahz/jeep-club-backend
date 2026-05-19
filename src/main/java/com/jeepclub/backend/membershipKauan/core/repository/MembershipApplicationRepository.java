package com.jeepclub.backend.membershipKauan.core.repository;

import com.jeepclub.backend.membershipKauan.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.membershipKauan.core.domain.model.MembershipApplication;

import java.util.Optional;

public interface MembershipApplicationRepository {

    MembershipApplication save(MembershipApplication application);

    Optional<MembershipApplication> findById(Long id);

    boolean existsByCpfAndStatus(String cpf, MembershipApplicationStatus status);

    boolean existsByEmailAndStatus(String email, MembershipApplicationStatus status);
}