package com.jeepclub.backend.iam.authorization.core.repository;
import com.jeepclub.backend.iam.authorization.api.module.RolePresentationQuery;
public interface RolePresentationQueryRepository { java.util.List<RolePresentationQuery.Details> findByIds(java.util.Collection<Long> ids); }
