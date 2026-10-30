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
        org.mockito.Mockito.when(authorities.findAuthorityCodesByUserId(owner)).thenReturn(List.of("VEHICLES_VEHICLE_ARCHIVE","VEHICLES_VEHICLE_DEACTIVATE","DEPENDENTS_DEPENDENT_ARCHIVE","DEPENDENTS_DEPENDENT_DEACTIVATE","PUBLICATIONS_EVENT_HEALTH_READ"));
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
}
