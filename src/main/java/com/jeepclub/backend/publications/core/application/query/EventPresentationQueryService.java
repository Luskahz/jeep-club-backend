package com.jeepclub.backend.publications.core.application.query;
import com.jeepclub.backend.publications.api.module.EventPresentationQuery;
import com.jeepclub.backend.publications.core.repository.EventPresentationQueryRepository;
@org.springframework.stereotype.Service @lombok.RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional(readOnly=true)
public class EventPresentationQueryService implements EventPresentationQuery {
    private final EventPresentationQueryRepository repository;
    public java.util.List<Details> findByIds(java.util.Collection<Long> ids) {
        if(ids.isEmpty())return java.util.List.of();
        if(ids.size()>500)throw new IllegalArgumentException("Maximum batch: 500");
        return repository.findByIds(ids);
    }
}
