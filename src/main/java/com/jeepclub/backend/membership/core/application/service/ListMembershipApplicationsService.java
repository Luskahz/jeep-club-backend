package com.jeepclub.backend.membership.core.application.service;

import com.jeepclub.backend.membership.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.membership.core.domain.model.MembershipApplication;
import com.jeepclub.backend.membership.core.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ListMembershipApplicationsService {

    private final MembershipApplicationRepository membershipApplicationRepository;

    @Transactional(readOnly = true)
    public Page<MembershipApplication> listAll(Pageable pageable) {
        return membershipApplicationRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<MembershipApplication> listByStatus(MembershipApplicationStatus status, Pageable pageable) {
        return membershipApplicationRepository.findAllByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<MembershipApplication> findById(Long id) {
        return membershipApplicationRepository.findById(id);
    }
}