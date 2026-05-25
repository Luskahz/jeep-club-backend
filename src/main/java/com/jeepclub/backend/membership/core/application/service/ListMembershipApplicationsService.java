package com.jeepclub.backend.membership.core.application.service;

import com.jeepclub.backend.membership.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.membership.core.domain.model.MembershipApplication;
import com.jeepclub.backend.membership.core.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ListMembershipApplicationsService {

    private final MembershipApplicationRepository membershipApplicationRepository;

    @Transactional(readOnly = true)
    public List<MembershipApplication> listAll() {
        return membershipApplicationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<MembershipApplication> listByStatus(MembershipApplicationStatus status) {
        return membershipApplicationRepository.findAllByStatus(status);
    }

    @Transactional(readOnly = true)
    public Optional<MembershipApplication> findById(Long id) {
        return membershipApplicationRepository.findById(id);
    }
}