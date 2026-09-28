package com.jeepclub.backend.iam.authorization.core.application.query;
import com.jeepclub.backend.iam.authorization.api.module.RolePresentationQuery;
import com.jeepclub.backend.iam.authorization.core.repository.RolePresentationQueryRepository;
@org.springframework.stereotype.Service @lombok.RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly=true)
public class RolePresentationQueryService implements RolePresentationQuery {
    private final RolePresentationQueryRepository repository;
    public java.util.List<Details> findByIds(java.util.Collection<Long> ids) {
        if(ids.isEmpty())return java.util.List.of();
        if(ids.size()>500)throw new IllegalArgumentException("Maximum batch: 500");
        return repository.findByIds(ids);
    }
}
