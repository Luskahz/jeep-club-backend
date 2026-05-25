package com.jeepclub.backend.membership.core.repository;

import com.jeepclub.backend.membership.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.membership.core.domain.model.MembershipApplication;

import java.util.List;
import java.util.Optional;

public interface MembershipApplicationRepository {

    MembershipApplication save(MembershipApplication application);

    Optional<MembershipApplication> findById(Long id);

    Optional<MembershipApplication> findByCpf(String cpf);

    List<MembershipApplication> findAll();

    List<MembershipApplication> findAllByStatus(MembershipApplicationStatus status);

    boolean existsByCpf(String cpf);
}