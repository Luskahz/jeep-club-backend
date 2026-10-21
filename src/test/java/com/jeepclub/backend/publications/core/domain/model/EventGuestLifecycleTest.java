package com.jeepclub.backend.publications.core.domain.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

class EventGuestLifecycleTest {
    final Instant now=Instant.parse("2026-10-01T12:00:00.123456Z");
    EventGuestRequest personal() {return EventGuestRequest.requested(1L,10L,100L,101L,"123.456.789-01","Guest",now).identified(1L);}
    EventGuestRequest admin() {return EventGuestRequest.standby(1L,99L,"12345678901","Guest",now).identified(2L);}
    @Test void pendingPersonalRequestHasPreferenceButDoesNotOccupyTransport() {
        var guest=personal();assertThat(guest.requestedVehicleId()).isEqualTo(101L);assertThat(guest.vehicleId()).isNull();
        assertThat(guest.transportStatus()).isEqualTo(EventGuestRequest.TransportStatus.NONE);assertThat(guest.origin()).isEqualTo(EventGuestRequest.Origin.MEMBER);
    }
    @Test void personalCancellationPreservesOriginAndOriginalReviewButCannotReactivate() {
        var reviewed=personal().review(true,100L,10L,99L,null,now.plusSeconds(1));var ended=reviewed.cancel();
        assertThat(ended.originRegistrationId()).isEqualTo(100L);assertThat(ended.requesterUserId()).isEqualTo(10L);
        assertThat(ended.reviewedAt()).isEqualTo(reviewed.reviewedAt());assertThat(ended.reviewerId()).isEqualTo(99L);
        assertThatThrownBy(ended::reactivate).isInstanceOf(IllegalStateException.class);
    }
    @Test void rejectionIsDistinctFromCancellationAndCannotBeReinterpreted() {
        var rejected=personal().review(false,null,null,99L,"Original refusal",now);
        assertThat(rejected.status()).isEqualTo(EventGuestRequest.Status.REJECTED);assertThat(rejected.rejectionReason()).isEqualTo("Original refusal");
        assertThatThrownBy(rejected::cancel).isInstanceOf(IllegalStateException.class);assertThatThrownBy(rejected::reactivate).isInstanceOf(IllegalStateException.class);
    }
    @Test void administrativeTransportIsIndependentFromParticipationAndReactivationForgetsAssociation() {
        var assigned=admin().allocate(101L,100L,10L,200L);var standby=assigned.unassign();
        assertThat(standby.status()).isEqualTo(EventGuestRequest.Status.APPROVED);assertThat(standby.transportStatus()).isEqualTo(EventGuestRequest.TransportStatus.STANDBY);
        var returned=assigned.cancel().reactivate();assertThat(returned).isEqualTo(admin());
        assertThat(returned.responsibleUserId()).isNull();assertThat(returned.selectedOfferId()).isNull();
    }
    @Test void cancelledAdministrativeGuestCannotReceiveAllocationOrPersonalReview() {
        var ended=admin().cancel();assertThatThrownBy(() -> ended.allocate(101L,100L,10L,null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> admin().review(true,100L,10L,99L,null,now)).isInstanceOf(IllegalStateException.class);
    }
    @Test void allocationRequiresRegistrationAndResponsibleMember() {
        assertThatThrownBy(() -> admin().allocate(101L,null,10L,null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> admin().allocate(101L,100L,null,null)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void withdrawnSelectedRideRetainsItsFinanciallyUnrelatedDecisionDates() {
        var offer=new EventRideOffer(200L,1L,2L,100L,10L,101L,EventRideOffer.Status.ACCEPTED,now,null).select(now.plusSeconds(1));
        var withdrawn=offer.withdraw(now.plusSeconds(2));
        assertThat(withdrawn.status()).isEqualTo(EventRideOffer.Status.WITHDRAWN);assertThat(withdrawn.respondedAt()).isEqualTo(now);
        assertThat(withdrawn.selectedAt()).isEqualTo(offer.selectedAt());assertThat(withdrawn.withdrawnAt()).isEqualTo(now.plusSeconds(2));
        assertThatThrownBy(() -> withdrawn.select(now.plusSeconds(3))).isInstanceOf(IllegalStateException.class);
    }
    @Test void acceptedRideWithdrawalIsNotAMemberDeclineAndTimeCannotRegress() {
        var offer=new EventRideOffer(200L,1L,2L,100L,10L,101L,EventRideOffer.Status.ACCEPTED,now,null);
        assertThat(offer.withdraw(now).selectedAt()).isNull();assertThat(offer.withdraw(now).status()).isNotEqualTo(EventRideOffer.Status.DECLINED);
        assertThatThrownBy(() -> offer.withdraw(now.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
    }
}
