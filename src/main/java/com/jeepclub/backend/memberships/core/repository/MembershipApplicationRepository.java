package com.jeepclub.backend.memberships.core.repository;

import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MembershipApplicationRepository {

    MembershipApplication save(MembershipApplication application);

    Optional<MembershipApplication> findById(Long id);

    Optional<MembershipApplication> findByCpf(String cpf);

    List<MembershipApplication> findAll();

    List<MembershipApplication> findAllByStatus(MembershipApplicationStatus status);

    Page<MembershipApplication> findAll(Pageable pageable);

    Page<MembershipApplication> findAllByStatus(MembershipApplicationStatus status, Pageable pageable);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    Optional<MembershipApplication> findByCpfAndStatus(
            String cpf,
            MembershipApplicationStatus status
    );

    boolean existsByEmailAndStatus(
            String email,
            MembershipApplicationStatus status
    );

}