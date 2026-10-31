package com.jeepclub.backend.publications.core.domain.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class EventRegistrationInvariantTest {
    final Instant now=Instant.parse("2026-10-01T12:00:00Z");
    EventRegistration registration(List<EventRegistration.Allocation> allocations,List<Long> unallocated) {
        return new EventRegistration(1L,1L,10L,EventRegistration.Status.PENDING_PAYMENT,allocations,now,null,null,unallocated);
    }
    @Test void dependentCannotAppearBothAllocatedAndUnallocatedOrTwiceUnallocated() {
        assertThatIllegalArgumentException().isThrownBy(() -> registration(
                List.of(new EventRegistration.Allocation(101L,true,List.of(201L))),List.of(201L)));
        assertThatIllegalArgumentException().isThrownBy(() -> registration(List.of(),List.of(201L,201L)));
    }
    @Test void duplicateVehicleOrAllocationWithoutMemberIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> registration(
                List.of(new EventRegistration.Allocation(101L,true,List.of()),new EventRegistration.Allocation(101L,false,List.of())),List.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> registration(
                List.of(new EventRegistration.Allocation(101L,false,List.of(201L))),List.of()));
    }
    @Test void snapshotsAreImmutableAndCancelledRegistrationCannotConfirmOrReallocate() {
        var deps=new ArrayList<>(List.of(201L));
        var allocations=new ArrayList<>(List.of(new EventRegistration.Allocation(101L,true,deps)));
        var unallocated=new ArrayList<>(List.of(202L));
        var registration=registration(allocations,unallocated);
        deps.clear(); allocations.clear(); unallocated.clear();
        assertThat(registration.allocations()).hasSize(1);
        assertThat(registration.allocations().get(0).dependentIds()).containsExactly(201L);
        assertThat(registration.unallocatedDependentIds()).containsExactly(202L);
        var confirmed=registration.confirm(now.plusSeconds(1));
        assertThat(confirmed.confirm(now.plusSeconds(2))).isSameAs(confirmed);
        assertThat(confirmed.confirmedAt()).isEqualTo(now.plusSeconds(1));
        var cancelled=confirmed.cancel(now.plusSeconds(3));
        assertThat(cancelled.confirm(now.plusSeconds(4))).isSameAs(cancelled);
        assertThat(cancelled.confirmedAt()).isEqualTo(now.plusSeconds(1));
        assertThat(cancelled.cancelledAt()).isEqualTo(now.plusSeconds(3));
        assertThatIllegalStateException().isThrownBy(() -> cancelled.allocate(List.of(),List.of()));
        assertThatIllegalStateException().isThrownBy(() -> cancelled.cancel(now.plusSeconds(4)));
    }
}
