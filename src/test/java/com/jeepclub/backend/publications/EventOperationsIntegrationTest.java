package com.jeepclub.backend.publications;

import com.jeepclub.backend.publications.core.application.service.*;
import com.jeepclub.backend.publications.core.application.exception.EventOperationException;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.domain.enums.*;
import com.jeepclub.backend.publications.core.repository.EventOperationsRepository;
import com.jeepclub.backend.publications.infra.persistence.jpa.*;
import com.jeepclub.backend.billing.api.module.*;
import com.jeepclub.backend.billing.core.domain.model.*;
import com.jeepclub.backend.billing.core.domain.enums.*;
import com.jeepclub.backend.billing.core.domain.enums.cycle.*;
import com.jeepclub.backend.billing.core.domain.enums.payment.*;
import com.jeepclub.backend.billing.core.repository.*;
import com.jeepclub.backend.billing.core.application.service.memberpayment.AdminMemberPaymentService;
import com.jeepclub.backend.vehicles.api.module.EventVehicleQuery;
import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.health.api.module.medicalprofile.*;
import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.platform.logging.SystemLogJpaRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.domain.Pageable;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest @ActiveProfiles("test") @Import(EventOperationsIntegrationTest.TimeConfiguration.class)
class EventOperationsIntegrationTest {
    static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    static final Instant START = NOW.plus(Duration.ofDays(10));
    static final BigDecimal AMOUNT = new BigDecimal("25.00");
    @Autowired AdminEventService admin;
    @Autowired EventService member;
    @Autowired EventBillingCommand billing;
    @Autowired EventFinancialQuery financial;
    @Autowired EventChargeCatalogQuery catalog;
    @Autowired ChargeDefinitionRepository definitions;
    @Autowired MemberChargeRepository charges;
    @Autowired MemberPaymentRepository payments;
    @Autowired MemberRefundRepository refunds;
    @Autowired EventChargeContextRepository contexts;
    @Autowired AdminMemberPaymentService paymentAdmin;
    @Autowired com.jeepclub.backend.billing.core.application.service.memberpayment.MemberPaymentService paymentMember;
    @Autowired com.jeepclub.backend.billing.core.application.service.memberrefund.MemberRefundService refundMember;
    @Autowired com.jeepclub.backend.billing.core.application.service.memberrefund.AdminMemberRefundService refundAdmin;
    @Autowired com.jeepclub.backend.billing.core.application.service.chargecycle.AdminChargeCycleService cycleAdmin;
    @Autowired MembershipChargeQuery membershipFinancial;
    @Autowired ChargeAssignmentRepository assignments;
    @Autowired EventOperationsRepository operations;
    @Autowired PublicationHistoryJpaRepository history;
    @Autowired SystemLogJpaRepository logs;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired MutableClock clock;
    @MockitoBean ImageMediaService media;
    @MockitoBean EventVehicleQuery vehicles;
    @MockitoBean DependentsQuery dependents;
    @MockitoBean EmergencyMedicalProfileQuery health;
    @MockitoBean com.jeepclub.backend.shared.storage.FileStorage storage;
    @MockitoBean com.jeepclub.backend.billing.core.port.BillingMembershipPort memberTargets;
    @MockitoBean com.jeepclub.backend.billing.core.port.BillingAuthorizationPort roleTargets;

    @BeforeEach void setup() {
        clock.now = NOW;
        when(storage.store(any(),anyString())).thenAnswer(a -> new com.jeepclub.backend.shared.storage.StoredFile("billing/payment-receipts/" + UUID.randomUUID() + ".pdf"));
        when(vehicles.findActive(anyLong())).thenAnswer(a -> Optional.of(new EventVehicleQuery.VehicleCapacity(a.getArgument(0), 10L, 3)));
        when(vehicles.findActiveBatch(anyCollection())).thenAnswer(a -> ((Collection<Long>) a.getArgument(0)).stream().map(id -> new EventVehicleQuery.VehicleCapacity(id, 10L, 3)).toList());
        when(dependents.isActiveDependentOfUser(anyLong(), eq(10L))).thenReturn(true);
    }
    Event event(boolean required) {
        var rule = new AdminEventService.ChargeConfiguration(null, "Event " + UUID.randomUUID(), null, AMOUNT, false, required, null);
        var event = admin.create(99L, "Trail", "Route", List.of(new PublicationImage("images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg", 0, true)), START, START.plusSeconds(3600), List.of(rule));
        return admin.publish(event.getId());
    }
    Event freeEvent() {
        var event = admin.create(99L, "Trail", "Route", List.of(new PublicationImage("images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg", 0, true)), START, START.plusSeconds(3600), List.of());
        return admin.publish(event.getId());
    }
    Event eventWithFinancialDueDate(boolean required, LocalDate financialDueDate) {
        var rule = new AdminEventService.ChargeConfiguration(null, "Event " + UUID.randomUUID(), null,
            AMOUNT, false, required, START.minusSeconds(3600), financialDueDate);
        var event = admin.create(99L, "Trail", "Route",
            List.of(new PublicationImage("images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg", 0, true)),
            START, START.plusSeconds(3600), List.of(rule));
        return admin.publish(event.getId());
    }
    List<EventRegistration.Allocation> allocation() { return List.of(new EventRegistration.Allocation(101L, true, List.of(201L))); }
    <T> T tx(Supplier<T> work) { return new TransactionTemplate(transactionManager).execute(s -> work.get()); }
    Long receipt(Long eventId, Long user) {
        var charge = financial.findByEvent(eventId).stream().filter(s -> s.userId().equals(user)).findFirst().orElseThrow();
        return tx(() -> payments.save(MemberPayment.submitForValidation(charge.memberChargeId(), AMOUNT,
            PaymentMethod.values()[0], clock.now, "receipt/key", null, clock.now)).getId());
    }

    @Test void independentLifecycleAndHistoryRetainOperations() {
        var e = freeEvent();
        assertThat(e.getEventStatus()).isEqualTo(EventStatus.OPEN);
        assertThat(member.register(e.getId(), 10L, allocation()).status()).isEqualTo(EventRegistration.Status.CONFIRMED);
        clock.now = START;
        var finished = admin.finish(e.getId());
        assertThat(finished.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(finished.effectiveStatus(clock.now)).isEqualTo(EventStatus.FINISHED);
        admin.delete(e.getId(), 99L);
        assertThat(tx(() -> operations.registrations(e.getId()))).hasSize(1);
        assertThat(history.findAll().stream().filter(h -> h.getPublicationId().equals(e.getId())).count()).isEqualTo(1);
    }
    @Test void optionalChargeDoesNotGateParticipationAndIsCreatedImmediately() {
        var e = event(false);
        assertThat(member.register(e.getId(), 10L, List.of()).status()).isEqualTo(EventRegistration.Status.CONFIRMED);
        assertThat(financial.findByEvent(e.getId())).singleElement().satisfies(s -> assertThat(s.effectiveStatus()).isEqualTo("PENDING"));
    }
    @Test void explicitFinancialDueDateRoundTripsAndAppliesToOptionalMemberCharge() {
        var due = LocalDate.of(2026, 11, 30);
        var e = eventWithFinancialDueDate(false, due);
        var rule = tx(() -> operations.rules(e.getId()).get(0));
        assertThat(rule.financialDueDate()).isEqualTo(due);
        assertThat(rule.participationCutoff()).isEqualTo(START.minusSeconds(3600));
        assertThat(member.register(e.getId(), 10L, List.of()).status()).isEqualTo(EventRegistration.Status.CONFIRMED);
        var chargeId = financial.findByEvent(e.getId()).get(0).memberChargeId();
        var charge = tx(() -> charges.findById(chargeId).orElseThrow());
        assertThat(charge.getDueDate()).isEqualTo(due);
        assertThat(charge.getPaymentAcceptancePolicy()).isEqualTo(PaymentAcceptancePolicy.AFTER_DUE_DATE);
    }
    @Test void nullFinancialDueDateRoundTripsAndOptionalDebtRemainsPayableAfterReferenceDate() {
        var e = eventWithFinancialDueDate(false, null);
        assertThat(tx(() -> operations.rules(e.getId()).get(0).financialDueDate())).isNull();
        assertThat(member.register(e.getId(), 10L, List.of()).status()).isEqualTo(EventRegistration.Status.CONFIRMED);
        var chargeId = financial.findByEvent(e.getId()).get(0).memberChargeId();
        var charge = tx(() -> charges.findById(chargeId).orElseThrow());
        assertThat(charge.getDueDate()).isEqualTo(START.atZone(ZoneOffset.UTC).toLocalDate());
        assertThat(charge.getPaymentAcceptancePolicy()).isEqualTo(PaymentAcceptancePolicy.AFTER_DUE_DATE);
        assertThat(charge.acceptsPaymentOn(charge.getDueDate().plusMonths(2))).isTrue();
    }
    @Test void requiredReceiptBeforeCutoffStaysConfirmedAndCanBeResubmittedAfterFinancialDueDate() {
        var due = LocalDate.of(2026, 11, 30);
        var e = eventWithFinancialDueDate(true, due);
        assertThat(member.register(e.getId(), 10L, List.of()).status()).isEqualTo(EventRegistration.Status.PENDING_PAYMENT);
        var state = financial.findByEvent(e.getId()).get(0);
        var file = new com.jeepclub.backend.billing.core.port.payment.PaymentReceiptFile("receipt.pdf", "application/pdf", new byte[]{1});
        var payment = paymentMember.submitForValidation(10L, state.memberChargeId(), AMOUNT, PaymentMethod.PIX, NOW, file, null);
        clock.now = START;
        assertThat(member.mine(e.getId(), 10L).status()).isEqualTo(EventRegistration.Status.CONFIRMED);
        clock.now = Instant.parse("2026-12-01T12:00:00Z");
        paymentAdmin.reject(payment.id(), 99L, "Correct receipt");
        assertThat(member.mine(e.getId(), 10L).status()).isEqualTo(EventRegistration.Status.CONFIRMED);
        assertThat(tx(() -> charges.findById(state.memberChargeId()).orElseThrow().acceptsPaymentOn(LocalDate.now(clock)))).isTrue();
        paymentMember.updateSubmission(10L, payment.id(), AMOUNT, PaymentMethod.PIX, clock.now, file, null);
        paymentAdmin.confirm(payment.id(), 99L);
        assertThat(member.mine(e.getId(), 10L).status()).isEqualTo(EventRegistration.Status.CONFIRMED);
        assertThat(financial.findByEvent(e.getId()).get(0).effectiveStatus()).isEqualTo("PAID");
    }
    @Test void financialDueDateCannotChangeAfterRegistration() {
        var e = eventWithFinancialDueDate(false, LocalDate.of(2026, 11, 30));
        member.register(e.getId(), 10L, List.of());
        var original = tx(() -> operations.rules(e.getId()).get(0));
        var replacement = new AdminEventService.ChargeConfiguration(original.chargeDefinitionId(), null, null,
            null, false, false, original.participationCutoff(), LocalDate.of(2026, 12, 1));
        assertThatThrownBy(() -> admin.update(e.getId(), null, null, null, null, false, null, List.of(replacement)))
            .isInstanceOf(EventOperationException.class).satisfies(ex ->
                assertThat(((EventOperationException) ex).getCode()).isEqualTo("EVENT_CHARGE_CONFIGURATION_LOCKED"));
        assertThat(tx(() -> operations.rules(e.getId()).get(0).financialDueDate())).isEqualTo(original.financialDueDate());
    }
    @Test void existingOptionalChargeWithLimitedBillingPolicyRequiresExplicitFinancialDueDate() {
        var definition = tx(() -> definitions.save(ChargeDefinition.create("Existing " + UUID.randomUUID(), null,
            AMOUNT, ChargeRecurrenceType.ONE_TIME, false, PaymentAcceptancePolicy.UNTIL_DUE_DATE, null, NOW)));
        var e = freeEvent();
        var noFinalDate = new AdminEventService.ChargeConfiguration(definition.getId(), null, null, null,
            false, false, null, null);
        assertThatThrownBy(() -> admin.update(e.getId(), null, null, null, null, false, null, List.of(noFinalDate)))
            .isInstanceOf(EventOperationException.class).hasMessageContaining("charge invalid");
        var due = LocalDate.of(2026, 11, 30);
        var withDueDate = new AdminEventService.ChargeConfiguration(definition.getId(), null, null, null,
            false, false, null, due);
        admin.update(e.getId(), null, null, null, null, false, null, List.of(withDueDate));
        assertThat(tx(() -> operations.rules(e.getId()).get(0).financialDueDate())).isEqualTo(due);
        assertThat(member.register(e.getId(), 10L, List.of()).status()).isEqualTo(EventRegistration.Status.CONFIRMED);
        var chargeId = financial.findByEvent(e.getId()).get(0).memberChargeId();
        var charge = tx(() -> charges.findById(chargeId).orElseThrow());
        assertThat(charge.getDueDate()).isEqualTo(due);
        assertThat(charge.getPaymentAcceptancePolicy()).isEqualTo(PaymentAcceptancePolicy.UNTIL_DUE_DATE);
    }
    @Test void requiredReceiptConfirmsAndLateRejectionDoesNotDowngrade() {
        var e = event(true);
        assertThat(member.register(e.getId(), 10L, List.of()).status()).isEqualTo(EventRegistration.Status.PENDING_PAYMENT);
        Long payment = receipt(e.getId(), 10L);
        clock.now = START;
        assertThat(member.mine(e.getId(), 10L).status()).isEqualTo(EventRegistration.Status.CONFIRMED);
        clock.now = START.plusSeconds(86400);
        paymentAdmin.reject(payment, 99L, "Invalid receipt");
        assertThat(member.mine(e.getId(), 10L).status()).isEqualTo(EventRegistration.Status.CONFIRMED);
        var state = financial.findByEvent(e.getId()).get(0);
        assertThat(state.paymentStatus()).isEqualTo("REJECTED");
        assertThat(tx(() -> charges.findById(state.memberChargeId()).orElseThrow().acceptsPaymentOn(LocalDate.now(clock)))).isTrue();
    }
    @Test void receiptSubmittedAfterCutoffCannotConfirmEvenIfPaid() {
        var e = event(true); member.register(e.getId(), 10L, List.of());
        clock.now = START.plusSeconds(1);
        var payment = receipt(e.getId(), 10L); paymentAdmin.confirm(payment, 99L);
        assertThat(member.mine(e.getId(), 10L).status()).isEqualTo(EventRegistration.Status.PENDING_PAYMENT);
    }
    @Test void scheduleAndFinanceAreLockedAfterRegistration() {
        var e = event(false); member.register(e.getId(), 10L, List.of());
        assertThatThrownBy(() -> admin.update(e.getId(), null, null, null, START.plusSeconds(60), false, null, null))
            .isInstanceOf(EventOperationException.class).hasMessageContaining("configuration locked");
        assertThatThrownBy(() -> member.register(e.getId(), 10L, List.of())).hasMessageContaining("already registered");
        member.cancel(e.getId(), 10L);
        assertThat(member.mine(e.getId(), 10L).status()).isEqualTo(EventRegistration.Status.CANCELLED);
    }
    @Test void automaticScheduleClosesRegistrationAndHealthContextWithoutScheduler() {
        var e = freeEvent(); clock.now = START;
        assertThat(member.findById(e.getId()).effectiveStatus(clock.now)).isEqualTo(EventStatus.IN_PROGRESS);
        assertThatThrownBy(() -> member.register(e.getId(), 10L, List.of())).hasMessageContaining("registration closed");
        clock.now = START.plusSeconds(3600);
        assertThat(member.findById(e.getId()).effectiveStatus(clock.now)).isEqualTo(EventStatus.FINISHED);
    }
    @Test void capacityOwnershipAndDependentChecksRejectInvalidAllocation() {
        var e = freeEvent();
        assertThatThrownBy(() -> member.register(e.getId(), 11L, allocation())).hasMessageContaining("not owned");
        when(dependents.isActiveDependentOfUser(201L, 10L)).thenReturn(false);
        assertThatThrownBy(() -> member.register(e.getId(), 10L, allocation())).hasMessageContaining("participant not found");
        when(dependents.isActiveDependentOfUser(201L, 10L)).thenReturn(true);
        assertThatThrownBy(() -> member.register(e.getId(), 10L, List.of(new EventRegistration.Allocation(101L, true, List.of(1L,2L,3L)))))
            .hasMessageContaining("capacity exceeded");
    }
    @Test void guestApprovalUsesLastSeatAndHistoryWarnsWithoutBlocking() {
        var e = freeEvent(); member.register(e.getId(), 10L, allocation());
        var guest = member.requestGuest(e.getId(), 10L, 101L, "123.456.789-01");
        assertThat(admin.dashboard(e.getId()).totalPeople()).isEqualTo(2);
        admin.reviewGuest(e.getId(), guest.id(), 99L, true, null);
        var d = admin.dashboard(e.getId());
        assertThat(d.totalPeople()).isEqualTo(3); assertThat(d.availableSeats()).isZero();
        assertThat(admin.visits(guest.cpf())).isGreaterThanOrEqualTo(1);
        assertThatThrownBy(() -> admin.reviewGuest(e.getId(), guest.id(), 99L, true, null)).hasMessageContaining("processed");
        assertThatThrownBy(() -> member.requestGuest(e.getId(), 10L, 101L, "23456789012")).hasMessageContaining("limit reached");
    }
    @Test void guestCpfCannotDuplicateAndRejectedGuestDoesNotOccupySeat() {
        var e = freeEvent(); member.register(e.getId(), 10L, allocation());
        var g = member.requestGuest(e.getId(), 10L, 101L, "34567890123");
        assertThatThrownBy(() -> admin.createGuest(e.getId(), 99L, g.cpf())).hasMessageContaining("already exists");
        admin.reviewGuest(e.getId(), g.id(), 99L, false, "Declined");
        assertThat(admin.dashboard(e.getId()).availableSeats()).isEqualTo(1);
    }
    @Test void rideAcceptanceDoesNotReserveAndAdministrativeSelectionAllocates() {
        var e = freeEvent(); member.register(e.getId(), 10L, allocation());
        var g = admin.createGuest(e.getId(), 99L, "45678901234");
        assertThat(member.rideRequests(e.getId(), 10L)).contains(g.id());
        var offer = member.respond(e.getId(), g.id(), 10L, 101L, true);
        assertThat(admin.dashboard(e.getId()).availableSeats()).isEqualTo(1);
        admin.select(e.getId(), offer.id(), 99L);
        assertThat(admin.dashboard(e.getId()).availableSeats()).isZero();
    }
    @Test void lostRideCapacityIsControlledConflict() {
        var e = freeEvent(); member.register(e.getId(), 10L, allocation());
        var g = admin.createGuest(e.getId(), 99L, "56789012345");
        var offer = member.respond(e.getId(), g.id(), 10L, 101L, true);
        var other = member.requestGuest(e.getId(), 10L, 101L, "67890123456");
        admin.reviewGuest(e.getId(), other.id(), 99L, true, null);
        assertThatThrownBy(() -> admin.select(e.getId(), offer.id(), 99L)).hasMessageContaining("capacity changed");
    }
    @Test void emergencyAccessIsIndividualContextualAndDurablyAudited() {
        var e = freeEvent(); member.register(e.getId(), 10L, allocation());
        assertThatThrownBy(() -> admin.health(e.getId(), MedicalProfileOwner.USER, 10L, 99L)).hasMessageContaining("profile not found");
        clock.now = START;
        assertThatThrownBy(() -> admin.health(e.getId(), MedicalProfileOwner.USER, 20L, 99L)).hasMessageContaining("participant not found");
        assertThatThrownBy(() -> admin.health(e.getId(), MedicalProfileOwner.USER, 10L, 99L)).hasMessageContaining("profile not found");
        var profile = new EmergencyMedicalProfileQuery.Profile("O_POSITIVE", "private", null, null, null, null, null, null, null, null, null);
        when(health.find(any(), anyLong())).thenReturn(Optional.of(profile));
        clock.now=NOW; // Before the scheduled start: emergencies remain accessible.
        assertThat(admin.health(e.getId(), MedicalProfileOwner.DEPENDENT, 201L, 99L)).isEqualTo(profile);
        clock.now=START.plus(java.time.Duration.ofDays(2));
        assertThat(admin.health(e.getId(), MedicalProfileOwner.USER, 10L, 99L)).isEqualTo(profile);
        assertThat(logs.findAll().stream().filter(l -> l.getAction().equals("EVENT_HEALTH_EMERGENCY_READ") && l.getPath().contains("/" + e.getId() + "/"))).hasSize(5);
    }
    @Test void guestNameIsValidatedPersistedAndPreservedAfterReview() {
        var event=freeEvent();member.register(event.getId(),10L,allocation());
        assertThatThrownBy(()->admin.createGuest(event.getId(),99L,"12345678909"," ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->member.requestGuest(event.getId(),10L,101L,"12345678909","x".repeat(151))).isInstanceOf(IllegalArgumentException.class);
        var guest=member.requestGuest(event.getId(),10L,101L,"12345678909","  Convidado Legível  ");
        assertThat(guest.guestName()).isEqualTo("Convidado Legível");
        assertThat(admin.reviewGuest(event.getId(),guest.id(),99L,true,null).guestName()).isEqualTo("Convidado Legível");
    }
    @Test void emergencyRejectsDependentWhoseOwnershipChanged() {
        var event=freeEvent();member.register(event.getId(),10L,allocation());
        when(dependents.isActiveDependentOfUser(201L,10L)).thenReturn(false);
        assertThatThrownBy(()->admin.health(event.getId(),MedicalProfileOwner.DEPENDENT,201L,99L)).hasMessageContaining("participant not found");
        verify(health,never()).find(any(),anyLong());
    }
    @Test void cancellationDefersRefundUntilPaymentActuallyConfirmed() {
        var e = event(true); member.register(e.getId(), 10L, List.of());
        var p = receipt(e.getId(), 10L);
        admin.cancel(e.getId(), 99L);
        assertThat(tx(() -> refunds.existsActiveByMemberPaymentId(p))).isFalse();
        paymentAdmin.confirm(p, 99L);
        assertThat(tx(() -> refunds.existsActiveByMemberPaymentId(p))).isTrue();
        assertThatThrownBy(() -> paymentAdmin.confirm(p, 99L)).isInstanceOf(RuntimeException.class);
        var cycle = tx(() -> contexts.findByEvent(e.getId()).get(0).cycleId());
        assertThat(tx(() -> refunds.findByChargeCycleId(cycle, Pageable.unpaged()).getTotalElements())).isEqualTo(1);
    }
    @Test void cancelThenRejectCreatesNoRefund() {
        var e = event(true); member.register(e.getId(), 10L, List.of()); var p = receipt(e.getId(), 10L);
        admin.cancel(e.getId(), 99L); paymentAdmin.reject(p, 99L, "Invalid");
        assertThat(tx(() -> refunds.existsActiveByMemberPaymentId(p))).isFalse();
    }
    @Test void sameDefinitionAcrossEventsHasSeparateCyclesAndIncrementalDebt() {
        var a = event(false); var definition = admin.rules(a.getId()).get(0).chargeDefinitionId();
        var b = freeEvent();
        admin.update(b.getId(), null, null, null, null, false, null,
            List.of(new AdminEventService.ChargeConfiguration(definition, null, null, null, false, false, null)));
        member.register(a.getId(), 10L, List.of()); member.register(b.getId(), 20L, List.of());
        assertThat(financial.findByEvent(a.getId())).extracting(EventFinancialQuery.State::userId).containsExactly(10L);
        assertThat(financial.findByEvent(b.getId())).extracting(EventFinancialQuery.State::userId).containsExactly(20L);
        admin.cancel(a.getId(), 99L);
        assertThat(financial.findByEvent(b.getId()).get(0).effectiveStatus()).isEqualTo("PENDING");
    }
    @Test void catalogExcludesRecurringAndInactiveDefinitions() {
        var monthly = tx(() -> definitions.save(ChargeDefinition.create("Monthly " + UUID.randomUUID(), null, AMOUNT,
            ChargeRecurrenceType.MONTHLY, true, PaymentAcceptancePolicy.AFTER_DUE_DATE, null, NOW)));
        var e = event(false);
        assertThat(catalog.findEligible(Pageable.unpaged()).stream().map(EventChargeCatalogQuery.Entry::id)).doesNotContain(monthly.getId());
        assertThatThrownBy(() -> billing.ensureAssignment(e.getId(), monthly.getId())).isInstanceOf(EventBillingException.class);
    }
    @Test void concurrentRegistrationCreatesOneRow() throws Exception {
        var e = freeEvent();
        var outcomes = race(() -> member.register(e.getId(), 10L, List.of()));
        assertThat(outcomes.stream().filter(EventRegistration.class::isInstance)).hasSize(1);
        assertThat(tx(() -> operations.registrations(e.getId()))).hasSize(1);
    }
    @Test void multipleRequiredChargesRequireEveryReceiptAndOptionalRemainsUnpaid() {
        var e = event(true);
        var initial = admin.rules(e.getId()).get(0);
        admin.update(e.getId(), null, null, null, null, false, null, List.of(
            new AdminEventService.ChargeConfiguration(initial.chargeDefinitionId(),null,null,null,false,true,null),
            new AdminEventService.ChargeConfiguration(null,"Second " + UUID.randomUUID(),null,AMOUNT,false,true,null),
            new AdminEventService.ChargeConfiguration(null,"Optional " + UUID.randomUUID(),null,AMOUNT,true,false,null)));
        member.register(e.getId(),10L,List.of());
        var required = admin.rules(e.getId()).stream().filter(EventChargeRule::requiredForParticipation).toList();
        var a = financial.evaluate(e.getId(), required.get(0).chargeDefinitionId(), 10L);
        tx(() -> payments.save(MemberPayment.submitForValidation(a.memberChargeId(), AMOUNT, PaymentMethod.values()[0],NOW,"receipt/a",null,NOW)));
        assertThat(member.mine(e.getId(),10L).status()).isEqualTo(EventRegistration.Status.PENDING_PAYMENT);
        var b = financial.evaluate(e.getId(), required.get(1).chargeDefinitionId(), 10L);
        tx(() -> payments.save(MemberPayment.submitForValidation(b.memberChargeId(), AMOUNT, PaymentMethod.values()[0],NOW,"receipt/b",null,NOW)));
        assertThat(member.mine(e.getId(),10L).status()).isEqualTo(EventRegistration.Status.CONFIRMED);
        assertThat(financial.findByEvent(e.getId())).hasSize(3);
    }
    @Test void realPaymentSubmissionRejectionResubmissionAndRefundLifecycle() {
        var e = event(true); member.register(e.getId(),10L,List.of());
        var state = financial.findByEvent(e.getId()).get(0);
        var file = new com.jeepclub.backend.billing.core.port.payment.PaymentReceiptFile("receipt.pdf","application/pdf",new byte[]{1});
        var payment = paymentMember.submitForValidation(10L,state.memberChargeId(),AMOUNT,PaymentMethod.PIX,NOW,file,null);
        assertThat(member.mine(e.getId(),10L).status()).isEqualTo(EventRegistration.Status.CONFIRMED);
        clock.now = START.plusSeconds(86400);
        paymentAdmin.reject(payment.id(),99L,"Please correct");
        paymentMember.updateSubmission(10L,payment.id(),AMOUNT,PaymentMethod.PIX,clock.now,file,null);
        paymentAdmin.confirm(payment.id(),99L);
        assertThat(member.mine(e.getId(),10L).status()).isEqualTo(EventRegistration.Status.CONFIRMED);
        assertThat(financial.findByEvent(e.getId()).get(0).paymentSubmittedAt()).isEqualTo(clock.now);
        var refund = refundMember.requestByMemberPaymentId(10L,payment.id());
        refundAdmin.approve(refund.id(),99L);
        assertThat(refundAdmin.markAsRefunded(refund.id(),99L).status().name()).isEqualTo("REFUNDED");
        assertThatThrownBy(() -> refundMember.requestByMemberPaymentId(10L,payment.id())).isInstanceOf(RuntimeException.class);
    }
    @Test void dependentWithoutVehicleAndAllocationMutationReconcileDashboard() {
        var e = freeEvent();
        member.register(e.getId(),10L,List.of(),List.of(201L));
        assertThat(admin.dashboard(e.getId()).totalPeople()).isEqualTo(2);
        member.allocate(e.getId(),10L,allocation(),List.of());
        assertThat(admin.dashboard(e.getId()).availableSeats()).isEqualTo(1);
        var guest = member.requestGuest(e.getId(),10L,101L,"11223344556");
        admin.reviewGuest(e.getId(),guest.id(),99L,true,null);
        assertThatThrownBy(() -> member.allocate(e.getId(),10L,
            List.of(new EventRegistration.Allocation(101L,true,List.of(201L,202L))),List.of())).hasMessageContaining("capacity exceeded");
        assertThat(admin.dashboard(e.getId()).totalPeople()).isEqualTo(3);
    }
    @Test void concurrentEventsSharingDefinitionRemainIsolated() throws Exception {
        var a = event(false); var definition = admin.rules(a.getId()).get(0).chargeDefinitionId(); var b = freeEvent();
        admin.update(b.getId(),null,null,null,null,false,null,List.of(new AdminEventService.ChargeConfiguration(definition,null,null,null,false,false,null)));
        assertThat(race(List.of(() -> member.register(a.getId(),10L,List.of()), () -> member.register(b.getId(),20L,List.of())))).allMatch(EventRegistration.class::isInstance);
        assertThat(financial.findByEvent(a.getId())).extracting(EventFinancialQuery.State::userId).containsExactly(10L);
        assertThat(financial.findByEvent(b.getId())).extracting(EventFinancialQuery.State::userId).containsExactly(20L);
    }
    @Test void administrativeAudiencesAndMembershipRemainIndependentOfEventCycle() {
        var e = event(false); Long definition = admin.rules(e.getId()).get(0).chargeDefinitionId();
        when(memberTargets.findActiveMemberUserIds()).thenReturn(List.of(30L));
        when(memberTargets.existsActiveMemberByUserId(anyLong())).thenReturn(true);
        when(roleTargets.findUserIdsByRoleId(5L)).thenReturn(List.of(40L,30L));
        tx(() -> {
            assignments.save(com.jeepclub.backend.billing.core.domain.model.assignment.AllMembersChargeAssignment.create(definition,NOW));
            assignments.save(com.jeepclub.backend.billing.core.domain.model.assignment.UserChargeAssignment.create(definition,50L,NOW));
            assignments.save(com.jeepclub.backend.billing.core.domain.model.assignment.RoleChargeAssignment.create(definition,5L,NOW));
            return null;
        });
        cycleAdmin.generate(definition, "ADMIN-" + UUID.randomUUID(), LocalDate.now(clock),99L);
        member.register(e.getId(),10L,List.of());
        assertThat(financial.findByEvent(e.getId())).extracting(EventFinancialQuery.State::userId).containsExactly(10L);
        assertThat(membershipFinancial.evaluate(definition,30L)).isEqualTo(MembershipChargeResult.WITHIN_PAYMENT_PERIOD);
        assertThat(membershipFinancial.evaluate(definition,40L)).isEqualTo(MembershipChargeResult.WITHIN_PAYMENT_PERIOD);
        assertThat(membershipFinancial.evaluate(definition,50L)).isEqualTo(MembershipChargeResult.WITHIN_PAYMENT_PERIOD);
        assertThat(membershipFinancial.evaluate(definition,10L)).isEqualTo(MembershipChargeResult.CHARGE_NOT_FOUND);
    }
    @Test void repeatedConcurrentGuestApprovalCreatesOneApproval() throws Exception {
        var e = freeEvent(); member.register(e.getId(),10L,allocation());
        var g = member.requestGuest(e.getId(),10L,101L,"01234567890");
        assertThat(race(() -> admin.reviewGuest(e.getId(),g.id(),99L,true,null)).stream().filter(EventGuestRequest.class::isInstance)).hasSize(1);
    }
    @Test void concurrentLateConfirmCreatesSingleRefund() throws Exception {
        var e = event(false); member.register(e.getId(),10L,List.of()); var payment = receipt(e.getId(),10L);
        admin.cancel(e.getId(),99L);
        var results = race(() -> paymentAdmin.confirm(payment,99L));
        assertThat(results.stream().filter(RuntimeException.class::isInstance)).hasSize(1);
        var cycle = tx(() -> contexts.findByEvent(e.getId()).get(0).cycleId());
        assertThat(tx(() -> refunds.findByChargeCycleId(cycle,Pageable.unpaged()).getTotalElements())).isEqualTo(1);
    }
    @Test void concurrentCycleAndMemberChargeCreationIsIdempotent() throws Exception {
        var e = event(false); Long definition = admin.rules(e.getId()).get(0).chargeDefinitionId();
        var outcomes = race(() -> billing.ensureEventMemberCharge(e.getId(), definition, 10L, LocalDate.of(2026,10,11), 99L));
        assertThat(outcomes).allMatch(Long.class::isInstance);
        assertThat(outcomes.get(0)).isEqualTo(outcomes.get(1));
        assertThat(tx(() -> contexts.findByEvent(e.getId()))).hasSize(1);
        assertThat(financial.findByEvent(e.getId())).hasSize(1);
    }
    @Test void twoGuestApprovalsCannotTakeSameLastSeat() throws Exception {
        var e = freeEvent(); member.register(e.getId(), 10L, allocation());
        var a = member.requestGuest(e.getId(), 10L, 101L, "78901234567");
        var b = member.requestGuest(e.getId(), 10L, 101L, "89012345678");
        var outcomes = race(List.of(() -> admin.reviewGuest(e.getId(), a.id(), 99L, true, null), () -> admin.reviewGuest(e.getId(), b.id(), 99L, true, null)));
        assertThat(outcomes.stream().filter(EventGuestRequest.class::isInstance)).hasSize(1);
        assertThat(admin.dashboard(e.getId()).guestsApproved()).isEqualTo(1);
    }
    @Test void concurrentRideSelectionAndRepeatedApprovalAreSerialized() throws Exception {
        var e = freeEvent(); member.register(e.getId(), 10L, allocation());
        var g = admin.createGuest(e.getId(), 99L, "90123456789");
        var offer = member.respond(e.getId(), g.id(), 10L, 101L, true);
        var outcomes = race(() -> admin.select(e.getId(), offer.id(), 99L));
        assertThat(outcomes.stream().filter(EventRideOffer.class::isInstance)).hasSize(1);
        assertThat(admin.dashboard(e.getId()).guestsApproved()).isEqualTo(1);
    }
    List<Object> race(Supplier<?> action) throws Exception { return race(List.of(action, action)); }
    List<Object> race(List<Supplier<?>> actions) throws Exception {
        var executor = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            var futures = actions.stream().map(action -> executor.submit(() -> {
                start.await(); try { return action.get(); } catch (RuntimeException ex) { return ex; }
            })).toList();
            start.countDown(); var result = new ArrayList<Object>();
            for (var future : futures) result.add(future.get(20, TimeUnit.SECONDS));
            return result;
        } finally { executor.shutdownNow(); }
    }
    static class MutableClock extends Clock {
        volatile Instant now = NOW;
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
    @TestConfiguration static class TimeConfiguration {
        @Bean @Primary MutableClock eventTestClock() { return new MutableClock(); }
    }
}
