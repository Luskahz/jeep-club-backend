package com.jeepclub.backend.publications.api.module;
import java.util.List;
public interface EventQuery {
    boolean exists(Long eventId);
    List<Long> confirmedParticipants(Long eventId);
}
