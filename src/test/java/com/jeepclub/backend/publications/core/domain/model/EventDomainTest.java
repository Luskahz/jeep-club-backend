package com.jeepclub.backend.publications.core.domain.model;
import org.junit.jupiter.api.Test;
import com.jeepclub.backend.publications.core.domain.enums.*;
import java.time.Instant;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
class EventDomainTest {
    Instant now = Instant.parse("2026-10-01T00:00:00Z");
    Event event() { return Event.create(1L,"Title","Body",List.of(new PublicationImage("images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg",0,true)),now.plusSeconds(100),now); }
    @Test void validatesAgendaAndExplicitTransitions() {
        var e = event();
        assertThatThrownBy(() -> e.schedule(now.plusSeconds(100), now.plusSeconds(99), now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> e.finish(now)).isInstanceOf(IllegalStateException.class);
        e.publish(now); e.finish(now.plusSeconds(100));
        assertThat(e.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(e.getEventStatus()).isEqualTo(EventStatus.FINISHED);
        assertThatThrownBy(() -> e.cancel(now.plusSeconds(101))).isInstanceOf(IllegalStateException.class);
    }
    @Test void cancellationIsTerminalAndIndependentFromPublication() {
        var e = event(); e.cancel(now);
        assertThat(e.effectiveStatus(now.plusSeconds(1000))).isEqualTo(EventStatus.CANCELLED);
        assertThat(e.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThatThrownBy(() -> e.schedule(now.plusSeconds(200), null, now)).isInstanceOf(IllegalStateException.class);
    }
    @Test void occupantsCannotDuplicateBetweenVehicles() {
        assertThatThrownBy(() -> new EventRegistration(null, 1L, 1L, EventRegistration.Status.CONFIRMED,
            List.of(new EventRegistration.Allocation(1L,true,List.of()), new EventRegistration.Allocation(2L,true,List.of())), now, now, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventRegistration(null, 1L, 1L, EventRegistration.Status.CONFIRMED,
            List.of(new EventRegistration.Allocation(1L,true,List.of(5L)), new EventRegistration.Allocation(2L,false,List.of(5L))), now, now, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
