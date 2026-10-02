package com.jeepclub.backend.publications.core.domain.model;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class Event extends Publication {
    private Instant startsAt;
    private Instant endsAt;
    private com.jeepclub.backend.publications.core.domain.enums.EventStatus eventStatus = com.jeepclub.backend.publications.core.domain.enums.EventStatus.OPEN;

    private Event(Long id, Long authorUserId, String title, String content, PublicationStatus status,
                  Instant createdAt, Instant updatedAt, Instant publishedAt, Instant archivedAt,
                  List<PublicationImage> images, Instant startsAt) {
        super(id, authorUserId, title, content, status, createdAt, updatedAt, publishedAt, archivedAt, images);
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt");
    }

    public static Event create(Long authorUserId, String title, String content, List<PublicationImage> images,
                               Instant startsAt, Instant now) {
        return new Event(null, authorUserId, title, content, PublicationStatus.DRAFT, now, now, null, null, images, startsAt);
    }

    public static Event reconstitute(Long id, Long authorUserId, String title, String content, PublicationStatus status,
                                    Instant createdAt, Instant updatedAt, Instant publishedAt, Instant archivedAt,
                                    List<PublicationImage> images, Instant startsAt) {
        positive(id, "id");
        return new Event(id, authorUserId, title, content, status, createdAt, updatedAt, publishedAt, archivedAt, images, startsAt);
    }

    public Instant getStartsAt() { return startsAt; }

    public Instant getEndsAt() { return endsAt; }
    public com.jeepclub.backend.publications.core.domain.enums.EventStatus getEventStatus() { return eventStatus; }

    public static Event reconstitute(Long id, Long authorUserId, String title, String content, PublicationStatus status,
                                    Instant createdAt, Instant updatedAt, Instant publishedAt, Instant archivedAt,
                                    List<PublicationImage> images, Instant startsAt, Instant endsAt,
                                    com.jeepclub.backend.publications.core.domain.enums.EventStatus eventStatus) {
        var event = reconstitute(id, authorUserId, title, content, status, createdAt, updatedAt, publishedAt, archivedAt, images, startsAt);
        validateSchedule(startsAt, endsAt);
        event.endsAt = endsAt;
        event.eventStatus = Objects.requireNonNull(eventStatus, "eventStatus");
        return event;
    }

    public void schedule(Instant startsAt, Instant endsAt, Instant now) {
        if (effectiveStatus(now) != com.jeepclub.backend.publications.core.domain.enums.EventStatus.OPEN)
            throw new IllegalStateException("Only open events can be rescheduled.");
        validateSchedule(startsAt, endsAt);
        if (!startsAt.isAfter(now)) throw new IllegalArgumentException("Event must start in the future.");
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public com.jeepclub.backend.publications.core.domain.enums.EventStatus effectiveStatus(Instant now) {
        if (eventStatus == com.jeepclub.backend.publications.core.domain.enums.EventStatus.CANCELLED
                || eventStatus == com.jeepclub.backend.publications.core.domain.enums.EventStatus.FINISHED) return eventStatus;
        if (endsAt != null && !now.isBefore(endsAt)) return com.jeepclub.backend.publications.core.domain.enums.EventStatus.FINISHED;
        return now.isBefore(startsAt) ? com.jeepclub.backend.publications.core.domain.enums.EventStatus.OPEN
                : com.jeepclub.backend.publications.core.domain.enums.EventStatus.IN_PROGRESS;
    }

    public void finish(Instant now) {
        if (effectiveStatus(now) != com.jeepclub.backend.publications.core.domain.enums.EventStatus.IN_PROGRESS)
            throw new IllegalStateException("Only events in progress can be finished.");
        eventStatus = com.jeepclub.backend.publications.core.domain.enums.EventStatus.FINISHED;
    }

    public void cancel(Instant now) {
        var state = effectiveStatus(now);
        if (state == com.jeepclub.backend.publications.core.domain.enums.EventStatus.FINISHED
                || state == com.jeepclub.backend.publications.core.domain.enums.EventStatus.CANCELLED)
            throw new IllegalStateException("Event is already terminal.");
        eventStatus = com.jeepclub.backend.publications.core.domain.enums.EventStatus.CANCELLED;
    }

    private static void validateSchedule(Instant start, Instant end) {
        Objects.requireNonNull(start, "startsAt");
        if (end != null && !end.isAfter(start)) throw new IllegalArgumentException("endsAt must be after startsAt.");
    }
}
