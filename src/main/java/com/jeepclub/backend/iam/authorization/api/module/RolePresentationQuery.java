package com.jeepclub.backend.iam.authorization.api.module;
public interface RolePresentationQuery {
    record Details(Long id, String name) {}
    java.util.List<Details> findByIds(java.util.Collection<Long> ids);
}
