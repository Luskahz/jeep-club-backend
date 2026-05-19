package com.jeepclub.backend.membership.core.application.service;

import com.jeepclub.backend.authentication.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.authentication.core.domain.model.MembershipApplication;
import com.jeepclub.backend.authentication.core.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        return membershipApplicationRepository.findByStatus(status);
    }
}