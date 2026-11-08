package com.jeepclub.backend.iam.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.core.application.result.BirthdayResult;
import com.jeepclub.backend.iam.identity.core.repository.BirthdayQueryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BirthdayQueryService {
    private final BirthdayQueryRepository birthdays;

    @Transactional(readOnly = true)
    public List<BirthdayResult> findByMonth(int month) {
        return birthdays.findByMonth(month);
    }
}
