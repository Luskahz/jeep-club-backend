package com.jeepclub.backend.lifecycle;

import com.jeepclub.backend.iam.identity.core.domain.model.User;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import com.jeepclub.backend.vehicles.core.application.service.vehicle.*;
import com.jeepclub.backend.vehicles.core.domain.enums.*;
import com.jeepclub.backend.publications.core.application.service.*;
import com.jeepclub.backend.publications.core.domain.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test")
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class EventParticipantLifecycleIntegrationTest {
    static final AtomicLong IDS=new AtomicLong(86000000000L);
    @Autowired UserRepository users;
    @Autowired AdminVehicleService vehicles;
    @Autowired VehicleService memberVehicles;
    @Autowired AdminEventService admin;
    @Autowired EventService member;
    @Autowired TransactionTemplate transactions;
    @Autowired Clock clock;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.jeepclub.backend.platform.storage.image.ImageMediaService media;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository accounts;
    @Autowired com.jeepclub.backend.iam.authentication.core.repository.SessionRepository sessions;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.jeepclub.backend.platform.security.jwt.JwtTokenParser parser;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider authorities;
    Long owner, vehicle;
    @Autowired com.jeepclub.backend.dependents.core.application.service.dependent.DependentService dependents;
    @Autowired jakarta.persistence.EntityManager entityManager;

    Long dependent() {
        return dependents.create("Synthetic participant",String.valueOf(IDS.incrementAndGet()),LocalDate.of(2010,1,1),
            com.jeepclub.backend.dependents.core.domain.enums.RelationshipType.CHILD,null,owner).id();
    }
    void medical(Long id) {
        transactions.execute(s -> {
            var p=new com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileEntity();
            p.setOwnerType(com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType.DEPENDENT);
            p.setOwnerId(id);p.setBloodType(com.jeepclub.backend.health.core.domain.enums.BloodType.UNKNOWN);
            p.setCreatedAt(Instant.now(clock));p.setUpdatedAt(Instant.now(clock));entityManager.persist(p);return null;
        });
    }
    @Test void operationalDependentBlocksMemberAndAdminHttpWithoutChangingHealthOrRegistration() throws Exception {
        Long d=dependent();medical(d);var e=event();member.register(e.getId(),owner,List.of(),List.of(d));String token=token();
        var before=admin.dashboard(e.getId());
        for(String base:List.of("/dependents/","/users/"+owner+"/dependents/"))for(String action:List.of("disable","archive"))
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(base+d+"/"+action).header("Authorization",token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value("DEPENDENT_LIFECYCLE_CONFLICT"));
        assertThat(dependents.findById(d,owner).status().name()).isEqualTo("ACTIVE");
        assertThat(admin.dashboard(e.getId())).isEqualTo(before);
        assertThat(admin.health(e.getId(),com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner.DEPENDENT,d,owner)).isNotNull();
    }
    @ParameterizedTest @ValueSource(strings={"INACTIVE","ARCHIVED"})
    void legacyInactiveConfirmedParticipantKeepsEmergencyAccessWithOwnershipAndAudit(String state) {
        Long d=dependent();medical(d);var e=event();member.register(e.getId(),owner,List.of(),List.of(d));
        jdbc.update("update dependents_dependent set status=?,archived_at=case when ?='ARCHIVED' then created_at else null end where id=?",state,state,d);
        assertThat(admin.health(e.getId(),com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner.DEPENDENT,d,owner)).isNotNull();
        assertThat(jdbc.queryForObject("select count(*) from platform_logs where action='EVENT_HEALTH_EMERGENCY_READ' and path=? and outcome='SUCCESS'",Long.class,
            "/admin/events/"+e.getId()+"/health/DEPENDENT/"+d)).isEqualTo(1);
        Long stranger=transactions.execute(tx -> users.create(User.create("Synthetic stranger",null,null,String.valueOf(IDS.incrementAndGet()),null,null,null,Instant.now(clock))).getId());
        jdbc.update("update dependents_dependent set user_id=? where id=?",stranger,d);
        assertThatThrownBy(() -> admin.health(e.getId(),com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner.DEPENDENT,d,owner)).isInstanceOf(RuntimeException.class);
    }
    @Test void emergencyHonorsCancellationMissingProfileAndExistingTemporalContract() {
        Long d=dependent();var e=event();member.register(e.getId(),owner,List.of(new EventRegistration.Allocation(vehicle,true,List.of(d))));
        assertThatThrownBy(() -> admin.health(e.getId(),com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner.DEPENDENT,d,owner)).hasMessageContaining("profile not found");
        medical(d);member.cancel(e.getId(),owner);
        assertThatThrownBy(() -> admin.health(e.getId(),com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner.DEPENDENT,d,owner)).hasMessageContaining("participant not found");
        dependents.disable(d,owner);dependents.archive(d,owner,owner);
        Long historical=dependent();medical(historical);var finished=event();member.register(finished.getId(),owner,List.of(),List.of(historical));
        jdbc.update("update publication_events set event_status='FINISHED' where publication_id=?",finished.getId());
        dependents.archive(historical,owner,owner);
        assertThat(admin.health(finished.getId(),com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner.DEPENDENT,historical,owner)).isNotNull();
    }
    @Test void emergencyRecoveryRequiresTheSpecificHttpPermission() throws Exception {
        Long d=dependent();medical(d);var e=event();member.register(e.getId(),owner,List.of(),List.of(d));String token=token();
        jdbc.update("update dependents_dependent set status='INACTIVE' where id=?",d);
        String path="/admin/events/"+e.getId()+"/health/DEPENDENT/"+d;
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).header("Authorization",token))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        org.mockito.Mockito.when(authorities.findAuthorityCodesByUserId(owner)).thenReturn(List.of("DEPENDENTS_DEPENDENT_READ"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).header("Authorization",token))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void allocatedAndUnallocatedPendingDependentsCannotLeaveOperationalEvent(boolean allocated) {
        Long d=dependent();var e=event();member.register(e.getId(),owner,
            allocated?List.of(new EventRegistration.Allocation(vehicle,true,List.of(d))):List.of(),allocated?List.of():List.of(d));
        jdbc.update("update event_registrations set status='PENDING_PAYMENT' where event_id=?",e.getId());
        assertThatThrownBy(() -> dependents.disable(d,owner)).isInstanceOf(com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException.class);
        assertThatThrownBy(() -> dependents.archive(d,owner,owner)).isInstanceOf(com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException.class);
    }
    @ParameterizedTest @ValueSource(strings={"NONE","CANCELLED_REGISTRATION","CANCELLED_EVENT","FINISHED_EVENT","EXPIRED_EVENT"})
    void dependentHistoryAndNoParticipationAllowTransitions(String state) {
        Long d=dependent();
        if(!state.equals("NONE")) {
            var e=event();member.register(e.getId(),owner,List.of(),List.of(d));
            switch(state) {
                case "CANCELLED_REGISTRATION" -> member.cancel(e.getId(),owner);
                case "CANCELLED_EVENT" -> admin.cancel(e.getId(),owner);
                case "FINISHED_EVENT" -> jdbc.update("update publication_events set event_status='FINISHED' where publication_id=?",e.getId());
                case "EXPIRED_EVENT" -> jdbc.update("update publication_events set starts_at=?,ends_at=? where publication_id=?",
                    java.sql.Timestamp.from(Instant.now(clock).minusSeconds(7200)),java.sql.Timestamp.from(Instant.now(clock).minusSeconds(3600)),e.getId());
            }
        }
        dependents.disable(d,owner);dependents.disable(d,owner,owner);dependents.archive(d,owner,owner);dependents.archive(d,owner,owner);
        assertThat(dependents.findById(d,owner).status().name()).isEqualTo("ARCHIVED");
    }

    @BeforeEach void fixture() {
        owner=transactions.execute(s -> users.create(User.create("Synthetic participant",null,null,
            String.valueOf(IDS.incrementAndGet()),null,null,null,Instant.now(clock))).getId());
        long unique=IDS.incrementAndGet();
        vehicle=vehicles.createForOwner("Synthetic",null,"TST"+String.format("%04d",unique%10000),String.valueOf(unique),
            "Jeep","Test",2020,2020,"Blue",5,FuelType.DIESEL,2d,false,owner).getId();
    }
    Event event() {
        var start=Instant.now(clock).plusSeconds(3600);
        return admin.publish(admin.create(owner,"Trail","Route",List.of(new PublicationImage("images/2026/10/30/550e8400-e29b-41d4-a716-446655440000.jpg",0,true)),start,start.plusSeconds(3600),List.of()).getId());
    }
    List<EventRegistration.Allocation> allocation() { return List.of(new EventRegistration.Allocation(vehicle,true,List.of())); }
    void transition(boolean administrative,boolean archive) {
        if(administrative) {if(archive) vehicles.archive(vehicle,owner); else vehicles.deactivate(vehicle,owner);}
        else {if(archive) memberVehicles.archive(vehicle,owner); else memberVehicles.deactivate(vehicle,owner);}
    }
    String token() {
        var now=Instant.now(clock);
        var session=transactions.execute(s -> {
            accounts.create(com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount.create(owner,"synthetic-unusable-hash",now));
            return sessions.save(com.jeepclub.backend.iam.authentication.core.domain.model.Session.create(owner,Duration.ofHours(1),now));
        });
        String token="synthetic-event-"+UUID.randomUUID();
        org.mockito.Mockito.when(parser.parseAndValidate(token)).thenReturn(new com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser(owner,session.getId(),"Synthetic",now.plusSeconds(3600)));
        org.mockito.Mockito.when(authorities.findAuthorityCodesByUserId(owner)).thenReturn(List.of("VEHICLES_VEHICLE_ARCHIVE","VEHICLES_VEHICLE_DEACTIVATE","DEPENDENTS_DEPENDENT_ARCHIVE","DEPENDENTS_DEPENDENT_DEACTIVATE","PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ"));
        return "Bearer "+token;
    }
    @Test void vehicleMemberAndAdminHttpReturnTheSameProblemAndPreserveState() throws Exception {
        var e=event();member.register(e.getId(),owner,allocation());String token=token();
        for(String role:List.of("member","admin"))for(String action:List.of("deactivate","archive"))
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/vehicles/"+action+"/"+role+"/"+vehicle).header("Authorization",token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value("VEHICLE_LIFECYCLE_CONFLICT"));
        assertThat(vehicles.findById(vehicle).getStatus()).isEqualTo(VehicleStatus.ACTIVE);
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void operationalRegistrationBlocksBothTransitionsWithoutChangingCapacityOrParticipation(boolean administrative) {
        var e=event(); member.register(e.getId(),owner,allocation());
        var before=admin.dashboard(e.getId());
        for(boolean archive:List.of(false,true))
            assertThatThrownBy(() -> transition(administrative,archive))
                .isInstanceOf(com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException.class);
        assertThat(vehicles.findById(vehicle).getStatus()).isEqualTo(VehicleStatus.ACTIVE);
        assertThat(admin.dashboard(e.getId())).isEqualTo(before);
    }
    @ParameterizedTest @ValueSource(strings={"NONE","CANCELLED_REGISTRATION","CANCELLED_EVENT","EXPIRED_EVENT","FINISHED_EVENT","DELETED_EVENT"})
    void historyDoesNotBlockVehicleLifecycle(String state) {
        if(!state.equals("NONE")) {
            var e=event();member.register(e.getId(),owner,allocation());
            switch(state) {
                case "CANCELLED_REGISTRATION" -> member.cancel(e.getId(),owner);
                case "CANCELLED_EVENT", "DELETED_EVENT" -> {admin.cancel(e.getId(),owner);if(state.equals("DELETED_EVENT"))admin.delete(e.getId(),owner);}
                case "FINISHED_EVENT" -> jdbc.update("update publication_events set event_status='FINISHED' where publication_id=?",e.getId());
                case "EXPIRED_EVENT" -> jdbc.update("update publication_events set starts_at=?, ends_at=? where publication_id=?",
                    java.sql.Timestamp.from(Instant.now(clock).minusSeconds(7200)),java.sql.Timestamp.from(Instant.now(clock).minusSeconds(3600)),e.getId());
            }
        }
        transition(false,false);transition(true,true);transition(false,true);
        assertThat(vehicles.findById(vehicle).getStatus()).isEqualTo(VehicleStatus.ARCHIVED);
    }
    @Test void guestAndRideOperationRemainUnchangedAfterRejectedLifecycle() {
        var e=event();member.register(e.getId(),owner,allocation());
        var guest=admin.createGuest(e.getId(),owner,String.valueOf(IDS.incrementAndGet()),"Synthetic guest");
        var offer=member.respond(e.getId(),guest.id(),owner,vehicle,true);
        assertThatThrownBy(() -> transition(true,true)).isInstanceOf(RuntimeException.class);
        admin.select(e.getId(),offer.id(),owner);
        var before=admin.dashboard(e.getId());
        assertThatThrownBy(() -> transition(false,false)).isInstanceOf(RuntimeException.class);
        assertThat(admin.dashboard(e.getId())).isEqualTo(before);
        assertThat(before.guestsAllocated()).isEqualTo(1);assertThat(before.availableSeats()).isEqualTo(3);
    }
    @ParameterizedTest @ValueSource(strings={"PENDING_GUEST","APPROVED_GUEST","CANCELLED_GUEST","REJECTED_GUEST","ACCEPTED_OFFER","SELECTED_OFFER","DECLINED_OFFER","WITHDRAWN_OFFER"})
    void operationalGuestAndOfferReferencesAreClassifiedIndependentlyOfRegistration(String state) {
        var e=event();member.register(e.getId(),owner,allocation());
        if(state.endsWith("GUEST")) {
            var g=member.requestGuest(e.getId(),owner,vehicle,String.valueOf(IDS.incrementAndGet()),"Synthetic");
            if(state.equals("APPROVED_GUEST"))admin.reviewGuest(e.getId(),g.id(),owner,true,null);
            if(state.equals("CANCELLED_GUEST"))member.cancelGuest(e.getId(),g.id(),owner,"Synthetic");
            if(state.equals("REJECTED_GUEST"))admin.reviewGuest(e.getId(),g.id(),owner,false,"Synthetic");
        } else {
            var g=admin.createGuest(e.getId(),owner,String.valueOf(IDS.incrementAndGet()),"Synthetic");
            var offer=member.respond(e.getId(),g.id(),owner,vehicle,!state.equals("DECLINED_OFFER"));
            if(state.equals("SELECTED_OFFER"))admin.select(e.getId(),offer.id(),owner);
            if(state.equals("WITHDRAWN_OFFER"))jdbc.update("update event_ride_offers set status='WITHDRAWN',withdrawn_at=coalesce(selected_at,responded_at) where id=?",offer.id());
            jdbc.update("update event_guest_requests set status='CANCELLED',vehicle_id=null,approved_vehicle_id=null,allocated_registration_id=null,responsible_user_id=null,selected_offer_id=null where id=?",g.id());
        }
        // Deliberate residual legacy inconsistency: a terminal registration must
        // not hide an independent operational guest/offer, or block on history.
        jdbc.update("update event_registrations set status='CANCELLED' where event_id=?",e.getId());
        boolean operational=List.of("PENDING_GUEST","APPROVED_GUEST","ACCEPTED_OFFER","SELECTED_OFFER").contains(state);
        if(operational)assertThatThrownBy(() -> transition(false,true)).isInstanceOf(com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException.class);
        else assertThatCode(() -> transition(true,true)).doesNotThrowAnyException();
    }
}
