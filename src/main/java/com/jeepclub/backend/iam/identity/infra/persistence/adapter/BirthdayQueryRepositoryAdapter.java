package com.jeepclub.backend.iam.identity.infra.persistence.adapter;

import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.core.application.result.BirthdayResult;
import com.jeepclub.backend.iam.identity.core.repository.BirthdayQueryRepository;
import com.jeepclub.backend.iam.identity.infra.persistence.jpa.UserJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BirthdayQueryRepositoryAdapter implements BirthdayQueryRepository {
    private final UserJpaRepository users;

    public List<BirthdayResult> findByMonth(int month) {
        return users.findBirthdays(month, UserStatus.ACTIVE);
    }
}
