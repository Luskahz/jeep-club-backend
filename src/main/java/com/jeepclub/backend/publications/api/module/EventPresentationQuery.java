package com.jeepclub.backend.publications.api.module;
public interface EventPresentationQuery {
    /** Active Event metadata, falling back to its deletion snapshot by original event ID. */
    record Details(Long id, String title, java.time.Instant startsAt) {}
    java.util.List<Details> findByIds(java.util.Collection<Long> ids);
}
