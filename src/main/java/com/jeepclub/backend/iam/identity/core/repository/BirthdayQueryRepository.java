package com.jeepclub.backend.iam.identity.core.repository;

import com.jeepclub.backend.iam.identity.core.application.result.BirthdayResult;
import java.util.List;

public interface BirthdayQueryRepository {
    List<BirthdayResult> findByMonth(int month);
}
