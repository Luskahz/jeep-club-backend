package com.jeepclub.backend.publications.api.http;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Inherited HTTP contracts also run against each real database. New methods have no outer transaction. */
@TestPropertySource(properties="spring.jpa.properties.hibernate.session_factory.statement_inspector=com.jeepclub.backend.publications.api.http.EventParticipantDatabaseContract$SqlCapture")
abstract class EventParticipantDatabaseContract extends EventHttpIntegrationTest {
    @Autowired javax.sql.DataSource dataSource;
    @MockitoSpyBean com.jeepclub.backend.publications.core.repository.PublicationRepository publicationRepository;
    public static class SqlCapture implements org.hibernate.resource.jdbc.spi.StatementInspector {
        static final List<String> statements = new CopyOnWriteArrayList<>();
        public String inspect(String sql) { statements.add(sql.toLowerCase(Locale.ROOT)); return sql; }
    }
    void identities() {
        when(users.findByIds(anyCollection())).thenAnswer(call -> ((Collection<Long>) call.getArgument(0)).stream()
            .map(id -> new com.jeepclub.backend.iam.identity.api.module.UserDetails(id, "Member " + id, null, null, null, null, null, "profile.jpg", true, null, null, null)).toList());
    }
    @Test void publishedParticipantsReadThroughAConcurrentWriterWithoutTakingLocks() throws Exception {
        long event = guestEvent(); identities(); auth(10L);
        clearInvocations(vehicleLocks, publicationRepository);
        var executor = Executors.newSingleThreadExecutor();
        try (var writer = dataSource.getConnection()) {
            writer.setAutoCommit(false);
            try (var statement = writer.prepareStatement("select id from publications where id=? for update")) {
                statement.setLong(1, event);
                try (var row = statement.executeQuery()) { assertThat(row.next()).isTrue(); }
            }
            SqlCapture.statements.clear();
            try {
                executor.submit(() -> {
                    mvc.perform(token(get("/events/{id}/participants", event)))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
                    return null;
                }).get(5, TimeUnit.SECONDS); // Must finish while the writer still holds its lock.
                assertThat(SqlCapture.statements).isNotEmpty().noneMatch(sql -> sql.contains("for update") || sql.contains("for no key update"));
                verify(publicationRepository).findById(event);
                verify(publicationRepository, never()).findEventByIdForUpdate(anyLong());
                verifyNoInteractions(vehicleLocks);
                System.out.println("BACK-590 " + getClass().getSimpleName() + " lock evidence: HTTP completed before writer rollback; no locking SELECT or vehicle lifecycle lock");
            } finally { writer.rollback(); }
        } finally { executor.shutdownNow(); }
    }
    @Autowired com.jeepclub.backend.publications.core.application.service.EventService participantService;
    @Autowired com.jeepclub.backend.iam.identity.core.repository.UserRepository identityRepository;
    @Test void sqlPaginationAndIdentityBatchesHaveBoundedQueryCounts() throws Exception {
        long event = guestEvent();
        var realUsers = com.jeepclub.backend.iam.identity.core.application.service.user.UserQueryDatabaseFixture.query(identityRepository);
        doAnswer(call -> realUsers.findByIds(call.getArgument(0))).when(users).findByIds(anyCollection());
        var ids = new ArrayList<Long>(); ids.add(10L);
        for (long id = 100; id < 601; id++) ids.add(id);
        for (long id : ids) jdbc.update("insert into identity_users(id,name,cpf,status,created_at) values (?,?,?,'ACTIVE',?)",
            id, "Member " + id, String.format("%011d", id), java.sql.Timestamp.from(java.time.Instant.now(clock)));
        for (long user = 100; user < 602; user++) jdbc.update(
            "insert into event_registrations(event_id,user_id,status,created_at,version) values (?,?,?,?,0)",
            event, user, user == 601 ? "PENDING_PAYMENT" : "CONFIRMED", java.sql.Timestamp.from(java.time.Instant.now(clock)));
        jdbc.update("insert into event_registrations(event_id,user_id,status,created_at,version) values (?,?,?,?,0)",
            event, 9999L, "CANCELLED", java.sql.Timestamp.from(java.time.Instant.now(clock)));
        auth(10L);
        int small = measuredPage(event, 1, 1);
        int large = measuredPage(event, 20, 20);
        assertThat(large).as("SQL count independent of participant page size").isEqualTo(small);
        measuredPage(event, 501, 50); // Existing HTTP page-size cap remains in force.
        clearInvocations(users);
        SqlCapture.statements.clear();
        assertThat(participantService.participants(event, org.springframework.data.domain.PageRequest.of(0, 501)).getContent()).hasSize(501);
        var batches = org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(users, times(2)).findByIds(batches.capture());
        assertThat(batches.getAllValues()).extracting(Collection::size).containsExactly(500, 1);
        assertThat(SqlCapture.statements.stream().filter(sql -> sql.contains("identity_users"))).hasSize(2)
            .allSatisfy(sql -> assertThat(sql).contains(" in "));
        assertThat(SqlCapture.statements).noneMatch(sql -> sql.contains("event_registration_occupants") || sql.contains("event_registration_vehicles") || sql.contains("event_registration_unallocated_dependents"));
        assertThat(SqlCapture.statements.stream().filter(sql -> sql.contains("event_registrations") && !sql.contains("count(")))
            .allSatisfy(sql -> assertThat(sql).contains("order by").containsAnyOf("fetch first", "limit", "fetch next"));
        mvc.perform(token(get("/events/{id}/participants", event).param("size", "1").param("page", "1")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].userId").value(100));
        auth(10L, "PUBLICATIONS_FEED_READ", "PUBLICATIONS_PUBLICATION_READ");
        SqlCapture.statements.clear();
        mvc.perform(token(get("/publications/feed").param("type", "EVENT"))).andExpect(status().isOk());
        System.out.println("BACK-590 " + getClass().getSimpleName() + " feed registration SQL: " + SqlCapture.statements.stream().filter(sql -> sql.contains("event_registrations")).toList());
        assertThat(SqlCapture.statements.stream().filter(sql -> sql.contains("event_registrations"))).hasSize(1)
            .allSatisfy(sql -> assertThat(sql).contains("group by").contains("count("));
        mvc.perform(token(get("/publications/{id}", event))).andExpect(status().isOk()).andExpect(jsonPath("$.details.registrationCount").value(502));
        jdbc.update("update event_registrations set status='CANCELLED' where event_id=?", event);
        mvc.perform(token(get("/publications/{id}", event))).andExpect(status().isOk()).andExpect(jsonPath("$.details.registrationCount").value(0));
        System.out.println("BACK-590 SQL evidence: page=1 queries=" + small + ", page=20 queries=" + large + "; identity batches=500,1; feed aggregate=1");
    }
    int measuredPage(long event, int size, int returned) throws Exception {
        SqlCapture.statements.clear();
        mvc.perform(token(get("/events/{id}/participants", event).param("size", Integer.toString(size))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(502))
            .andExpect(jsonPath("$.content.length()").value(returned)).andExpect(jsonPath("$.content[0].userId").value(10));
        assertThat(SqlCapture.statements).noneMatch(sql -> sql.contains("for update"));
        System.out.println("BACK-590 " + getClass().getSimpleName() + " SQL page=" + size + ": " + SqlCapture.statements);
        return SqlCapture.statements.size();
    }
    @Test void missingDraftAndOtherSubtypeAreNotVisible() throws Exception {
        long event = guestEvent(); auth(10L);
        mvc.perform(token(get("/events/{id}/participants", Long.MAX_VALUE))).andExpect(status().isNotFound());
        jdbc.update("update publications set status='DRAFT',published_at=null where id=?", event);
        mvc.perform(token(get("/events/{id}/participants", event))).andExpect(status().isNotFound());
        var tx = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        long notice = tx.execute(status -> {
            var publication = com.jeepclub.backend.publications.core.domain.model.Notice.create(99L, "Notice", "Body",
                List.of(new com.jeepclub.backend.publications.core.domain.model.PublicationImage("images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg", 0, true)), java.time.Instant.now(clock));
            publication.publish(java.time.Instant.now(clock));
            return publicationRepository.save(publication).getId();
        });
        mvc.perform(token(get("/events/{id}/participants", notice))).andExpect(status().isNotFound());
    }
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
}
