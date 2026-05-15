package com.jeepclub.backend.authentication.core.repository;

import com.jeepclub.backend.authentication.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.authentication.core.domain.model.MembershipApplication;

import java.util.Optional;

public interface MembershipApplicationRepository {

    MembershipApplication save(MembershipApplication application);

    Optional<MembershipApplication> findById(Long id);

    boolean existsByCpfAndStatus(String cpf, MembershipApplicationStatus status);

    boolean existsByEmailAndStatus(String email, MembershipApplicationStatus status);
}