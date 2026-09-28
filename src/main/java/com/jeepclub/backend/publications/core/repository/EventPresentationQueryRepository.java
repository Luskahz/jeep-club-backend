package com.jeepclub.backend.publications.core.repository;
import com.jeepclub.backend.publications.api.module.EventPresentationQuery;
public interface EventPresentationQueryRepository { java.util.List<EventPresentationQuery.Details> findByIds(java.util.Collection<Long> ids); }
