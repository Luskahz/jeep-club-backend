package com.jeepclub.backend.memberships.registry;

import com.jeepclub.backend.memberships.core.application.service.member.*;
import com.jeepclub.backend.memberships.core.application.result.*;
import com.jeepclub.backend.memberships.api.module.*;
import com.jeepclub.backend.memberships.core.domain.model.*;
import com.jeepclub.backend.memberships.core.domain.exception.MemberRegistryException;
import com.jeepclub.backend.memberships.infra.persistence.jpa.*;
import com.jeepclub.backend.memberships.infra.persistence.mapper.MemberMapper;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.iam.identity.api.module.spi.UserMembershipNumberProjection;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.data.domain.PageRequest;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Statement;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/** The same application/JPA/transaction/concurrency scenarios run against H2 and real MySQL. */
public abstract class MemberRegistryScenarios {
    @Autowired protected AdminMemberService admin;
    @Autowired protected MemberRegistrationQuery query;
    @Autowired protected MemberRegistryImportService importer;
    @Autowired protected MemberJpaRepository members;
    @Autowired protected MemberRegistrySequenceJpaRepository sequences;
    @Autowired protected MembershipNumberClaimJpaRepository claims;
    @MockitoSpyBean protected MemberRegistryAuditJpaRepository audits;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired protected UserQuery identities;
    @MockitoSpyBean protected UserMembershipNumberProjection projection;
    @Autowired protected EntityManagerFactory emf;
    private static final AtomicInteger SOURCE=new AtomicInteger(910000000);
    protected Long first,second;
    @BeforeEach void setupRegistry() {
        if(!sequences.existsById(1L)) {
            var row=new com.jeepclub.backend.memberships.infra.persistence.entity.MemberRegistrySequenceEntity();row.setId(1L);sequences.saveAndFlush(row);
        }
        first=identity();second=identity();
    }
    @AfterEach void resetFaults() { reset(audits); }
    protected Long identity() {
        var key=new GeneratedKeyHolder();String cpf=cpf(SOURCE.incrementAndGet());
        jdbc.update(connection -> {
            var statement=connection.prepareStatement("insert into identity_users(name,cpf,status,created_at) values(?,?,'ACTIVE',?)",Statement.RETURN_GENERATED_KEYS);
            statement.setString(1,"Synthetic registry fixture");statement.setString(2,cpf);statement.setTimestamp(3,java.sql.Timestamp.from(Instant.now()));return statement;
        },key);
        return key.getKey().longValue();
    }
    protected static String cpf(int value) {
        String digits=String.format("%09d",value);
        for(int count=9;count<=10;count++) { int sum=0;for(int i=0;i<count;i++)sum+=(digits.charAt(i)-'0')*(count+1-i);int check=11-sum%11;digits+=check>=10?0:check; }
        return digits;
    }
    private String number() { return "H-"+first+"-027"; }
    @Test void dynamicCorrectionPreservesBothIdsIdentityAndAudit() {
        var before=identities.findById(first).orElseThrow();
        var member=admin.assign(first,"027",first,"Synthetic verified source");
        var changed=admin.correct(first,"037",member.version(),first,"Synthetic correction");
        assertThat(changed.id()).isEqualTo(member.id());assertThat(changed.identityUserId()).isEqualTo(first);
        assertThat(identities.findById(first)).contains(before);
        assertThat(query.byIdentityUserId(first).orElseThrow().membershipNumber()).isEqualTo("037");
        assertThat(projection.currentNumber(first)).isEqualTo("037");
        assertThat(query.byCurrentMembershipNumber("027")).isEmpty();
        var history=admin.history(first,PageRequest.of(0,20)).getContent();assertThat(history).hasSize(2);
        assertThat(history.get(0).previousNumber()).isEqualTo("027");assertThat(history.get(0).outcome()).isEqualTo("SUCCESS");
        assertThat(history.get(0).actorUserId()).isEqualTo(first);
    }
    @Test void userWithoutMembershipStaysValidAndHasNoFallback() {
        assertThat(query.byIdentityUserId(first)).isEmpty();assertThat(projection.currentNumber(first)).isNull();
        assertThat(identities.existsById(first)).isTrue();assertThat(query.currentNumbers(List.of(first,second))).isEmpty();
    }
    @Test void assignmentIsIdempotentAndDoesNotForgeAudit() {
        var a=admin.assign(first,number(),first,"Verified");long count=audits.count();
        assertThat(admin.assign(first,number(),first,"Repeated verified request").id()).isEqualTo(a.id());
        assertThat(audits.count()).isEqualTo(count);
        assertThatThrownBy(() -> admin.assign(first,number()+"X",first,"Another number")).isInstanceOf(MemberRegistryException.class);
        assertThatThrownBy(() -> admin.assign(second,number(),first,"Collision")).isInstanceOf(MemberRegistryException.class);
    }
    @Test void historicNumbersAndReservationsCannotBeReused() {
        var a=admin.assign(first,number(),first,"Verified");admin.correct(first,number()+"X",a.version(),first,"Correction");
        assertThatThrownBy(() -> admin.assign(second,number(),first,"Reuse")).isInstanceOf(MemberRegistryException.class);
        assertThatThrownBy(() -> admin.correct(first,number(),1,first,"Reuse")).isInstanceOf(MemberRegistryException.class);
        admin.reserve(number()+"R",first,"Synthetic reservation");
        assertThatThrownBy(() -> admin.assign(second,number()+"R",first,"Reserved")).isInstanceOf(MemberRegistryException.class);
    }
    @Test void leadingZerosArePreservedAndAmbiguousVariantsAreRejected() {
        String legacy="000000"+(500000+first);admin.assign(first,legacy,first,"Verified");
        assertThat(query.byIdentityUserId(first).orElseThrow().membershipNumber()).isEqualTo(legacy);
        assertThat(query.byCurrentMembershipNumber(Long.toString(500000+first))).isEmpty();
        assertThatThrownBy(() -> admin.assign(second,Long.toString(500000+first),first,"Ambiguous")).isInstanceOf(MemberRegistryException.class);
    }
    @Test void nonexistentInactiveAndInvalidInputsDoNotWrite() {
        long before=members.count();
        assertThatThrownBy(() -> admin.assign(Long.MAX_VALUE,number(),first,"Verified")).isInstanceOf(MemberRegistryException.class);
        jdbc.update("update identity_users set status='DISABLED',disabled_at=created_at,updated_at=created_at where id=?",second);
        assertThatThrownBy(() -> admin.assign(second,number(),first,"Inactive")).isInstanceOf(MemberRegistryException.class);
        assertThatThrownBy(() -> admin.assign(first," 027",first,"Whitespace")).isInstanceOf(MemberRegistryException.class);
        assertThatThrownBy(() -> admin.assign(first,number(),first," ")).isInstanceOf(MemberRegistryException.class);
        assertThat(members.count()).isEqualTo(before);
    }
    @Test void auditFailureRollsBackMemberAndClaim() {
        doThrow(new IllegalStateException("injected synthetic audit failure")).when(audits).saveAndFlush(any());
        assertThatThrownBy(() -> admin.assign(first,number(),first,"Verified")).isInstanceOf(RuntimeException.class).hasStackTraceContaining("injected");
        assertThat(query.byIdentityUserId(first)).isEmpty();assertThat(claims.existsById(new MembershipNumber(number()).claimKey())).isFalse();
    }
    @Test void issuanceFailureRollsBackCounterMemberClaimAndAudit() {
        var before=sequences.findById(1L).orElseThrow().getNextValue();
        doThrow(new IllegalStateException("injected audit failure")).when(audits).saveAndFlush(any());
        assertThatThrownBy(() -> admin.generate(first,first,"Synthetic approved issuance")).isInstanceOf(RuntimeException.class).hasStackTraceContaining("injected");
        assertThat(query.byIdentityUserId(first)).isEmpty();assertThat(sequences.findById(1L).orElseThrow().getNextValue()).isEqualTo(before);
    }
    @Test void staleCorrectionFailsWithoutPartialAudit() {
        var a=admin.assign(first,number(),first,"Verified");admin.correct(first,number()+"A",a.version(),first,"Correction");long count=audits.count();
        assertThatThrownBy(() -> admin.correct(first,number()+"B",a.version(),first,"Stale")).isInstanceOf(MemberRegistryException.class);
        assertThat(audits.count()).isEqualTo(count);assertThat(query.byIdentityUserId(first).orElseThrow().membershipNumber()).endsWith("A");
    }
    @Test void generatorSkipsReservedNumbersAndIsIndependentOfIds() {
        Long next=sequences.findById(1L).orElseThrow().getNextValue();if(next==null)next=100L;
        String reserved=String.format("%03d",next);admin.reserve(reserved,first,"Synthetic reservation");
        var a=admin.generate(first,first,"Synthetic issue");assertThat(Long.parseLong(a.membershipNumber())).isEqualTo(next+1);
        assertThat(admin.generate(first,first,"Retry").id()).isEqualTo(a.id());
    }
    @Test void concurrentGenerationIsUniqueAndSequential() throws Exception {
        var values=race(() -> admin.generate(first,first,"Concurrent issue").membershipNumber(),() -> admin.generate(second,second,"Concurrent issue").membershipNumber());
        assertThat(values).doesNotHaveDuplicates();
        assertThat(Math.abs(Long.parseLong(values.get(0))-Long.parseLong(values.get(1)))).isEqualTo(1);
    }
    @Test void concurrentRequestsForSameIdentityAreIdempotent() throws Exception {
        var values=race(() -> admin.generate(first,first,"Concurrent retry").id(),() -> admin.generate(first,first,"Concurrent retry").id());
        assertThat(values.get(0)).isEqualTo(values.get(1));assertThat(admin.history(first,PageRequest.of(0,10)).getTotalElements()).isEqualTo(1);
    }
    @Test void competingHistoricalAssignmentsHaveOneWinner() throws Exception {
        var values=race(() -> attempt(() -> admin.assign(first,number(),first,"Concurrent link")),() -> attempt(() -> admin.assign(second,number(),second,"Concurrent link")));
        assertThat(values).containsExactlyInAnyOrder("SUCCESS","CONFLICT");
    }
    @Test void competingCorrectionsHaveOneWinner() throws Exception {
        var a=admin.assign(first,number(),first,"Verified");
        var values=race(() -> attempt(() -> admin.correct(first,number()+"A",a.version(),first,"Concurrent correction")),() -> attempt(() -> admin.correct(first,number()+"B",a.version(),first,"Concurrent correction")));
        assertThat(values).containsExactlyInAnyOrder("SUCCESS","CONFLICT");assertThat(admin.history(first,PageRequest.of(0,10)).getTotalElements()).isEqualTo(2);
    }
    @Test void realDatabaseRejectsDuplicateIdentityAndNumberOutsideApplication() {
        admin.assign(first,number(),first,"Verified");
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).execute(status -> members.saveAndFlush(MemberMapper.toEntity(Member.create(first,number()+"X",Instant.now())))))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).execute(status -> members.saveAndFlush(MemberMapper.toEntity(Member.create(second,number(),Instant.now())))))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void batchOfOneHundredUsesOneQuery() {
        var ids=new ArrayList<Long>();
        for(int i=0;i<100;i++) { Long id=identity();ids.add(id);admin.assign(id,"B-"+id,id,"Synthetic batch fixture"); }
        var stats=emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();stats.setStatisticsEnabled(true);stats.clear();
        assertThat(query.currentNumbers(ids)).hasSize(100);assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        stats.setStatisticsEnabled(false);
    }
    @Test void dryRunNeverWritesAndReportsMissingAndUnverifiedIdentities() {
        var rows=List.of(row(first,number()),row(Long.MAX_VALUE,number()+"M"),new MemberImportRow(second,number()+"U",false,"synthetic-source"));
        long before=members.count(),auditCount=audits.count(),claimCount=claims.count();
        var report=importer.dryRun(rows,first,"Verified source");
        assertThat(report.rows()).extracting(MemberImportReport.Row::outcome).containsExactly("READY","IDENTITY_NOT_FOUND","UNVERIFIED");
        assertThat(members.count()).isEqualTo(before);assertThat(audits.count()).isEqualTo(auditCount);assertThat(claims.count()).isEqualTo(claimCount);
    }
    @Test void duplicateSourceIsNotSilentlyApplied() {
        var rows=List.of(row(first,number()),row(second,number()));var plan=importer.dryRun(rows,first,"Verified source");
        assertThat(importer.apply(rows,first,"Verified source",plan.dryRunToken()).rows()).extracting(MemberImportReport.Row::outcome).containsOnly("DUPLICATE_SOURCE");
        assertThat(query.byIdentityUserId(first)).isEmpty();
    }
    @Test void importAppliesPerUnitAndRetriesIdempotently() {
        var rows=List.of(row(first,number()),row(second,number()+"Z"));var plan=importer.dryRun(rows,first,"Verified source");
        Long other=identity();admin.assign(other,number(),other,"Concurrent conflict after preview");
        var applied=importer.apply(rows,first,"Verified source",plan.dryRunToken());
        assertThat(applied.rows()).extracting(MemberImportReport.Row::outcome).containsExactly("NUMBER_UNAVAILABLE","APPLIED");
        long count=audits.count();var retry=importer.dryRun(rows,first,"Verified source");
        assertThat(importer.apply(rows,first,"Verified source",retry.dryRunToken()).rows().get(1).outcome()).isEqualTo("ALREADY_LINKED");
        assertThat(audits.count()).isEqualTo(count);
    }
    @Test void applyingWithoutExactPreviewProofFails() {
        var rows=List.of(row(first,number()));var plan=importer.dryRun(rows,first,"Verified source");
        assertThatThrownBy(() -> importer.apply(rows,first,"Changed source",plan.dryRunToken())).isInstanceOf(MemberRegistryException.class);
        assertThatThrownBy(() -> importer.apply(rows,second,"Verified source",plan.dryRunToken())).isInstanceOf(MemberRegistryException.class);
        assertThatThrownBy(() -> importer.apply(rows,first,"Verified source",null)).isInstanceOf(MemberRegistryException.class);
        assertThat(query.byIdentityUserId(first)).isEmpty();
    }
    @Test void generationAndHistoricalAssignmentCompeteWithoutDuplicatingNumber() throws Exception {
        Long next=sequences.findById(1L).orElseThrow().getNextValue();if(next==null)next=100L;
        String candidate=String.format("%03d",next);
        var outcomes=race(() -> attempt(() -> admin.assign(first,candidate,first,"Synthetic race")),
                () -> attempt(() -> admin.generate(second,second,"Synthetic race")));
        assertThat(outcomes.get(1)).isEqualTo("SUCCESS");
        assertThat(outcomes.get(0)).isIn("SUCCESS","CONFLICT");
        var generated=query.byIdentityUserId(second).orElseThrow();
        query.byIdentityUserId(first).ifPresent(manual -> assertThat(manual.membershipNumber()).isNotEqualTo(generated.membershipNumber()));
    }
    @Test void auditColumnsCannotBeRewrittenThroughJpa() {
        var member=admin.assign(first,number(),first,"Original audit reason");
        var history=admin.history(first,PageRequest.of(0,10)).getContent().get(0);
        var event=audits.findById(history.id()).orElseThrow();event.setReason("Attempted rewrite");audits.saveAndFlush(event);
        assertThat(audits.findById(history.id()).orElseThrow().getReason()).isEqualTo("Original audit reason");
    }
    @Autowired protected com.jeepclub.backend.iam.identity.core.application.service.user.AdminUserService adminUsers;
    @Test void hundredUserProjectionUsesOnlyAuxiliaryIdsAndOneMembershipLookup() {
        for(int i=0;i<100;i++) { Long id=identity();admin.assign(id,"PAGE-"+id,id,"Synthetic batch fixture"); }
        var stats=emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();stats.setStatisticsEnabled(true);stats.clear();
        try {
            var filter=new com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserFilter(null,null,null,null,null,null,null,null,null,null,null,null,null);
            var page=adminUsers.findAll(filter,java.util.Set.of(com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserField.MEMBERSHIP_NUMBER),PageRequest.of(0,100));
            assertThat(page.getContent()).hasSize(100).allSatisfy(u -> {assertThat(u.id()).isNotNull();assertThat(u.name()).isNull();assertThat(u.cpf()).isNull();});
            projection.currentNumbers(page.getContent().stream().map(u -> u.id()).toList());
            assertThat(stats.getPrepareStatementCount()).isEqualTo(3); // tuple page, count, single registry IN
        } finally { stats.setStatisticsEnabled(false); }
    }
    @Test void concurrentImportsOfSameBindingAreIdempotentAndAuditedOnce() throws Exception {
        var rows=List.of(row(first,number()));String reason="Synthetic concurrent source";
        var plan=importer.dryRun(rows,first,reason);long count=audits.count();
        var result=race(() -> importer.apply(rows,first,reason,plan.dryRunToken()),() -> importer.apply(rows,first,reason,plan.dryRunToken()));
        assertThat(result).allSatisfy(r -> assertThat(r.rows().get(0).outcome()).isIn("APPLIED","ALREADY_LINKED"));
        assertThat(query.byIdentityUserId(first)).isPresent();assertThat(audits.count()).isEqualTo(count+1);
        var event=admin.history(first,PageRequest.of(0,10)).getContent().get(0);
        assertThat(event.actorUserId()).isEqualTo(first);assertThat(event.reason()).contains("IMPORT synthetic-source-verified:",reason);
    }
    @Test void unconfirmedImportAndChangedSourceCannotWrite() {
        var unconfirmed=List.of(new MemberImportRow(first,number(),false,"synthetic-source"));
        var plan=importer.dryRun(unconfirmed,first,"Synthetic source");
        assertThat(importer.apply(unconfirmed,first,"Synthetic source",plan.dryRunToken()).rows().get(0).outcome()).isEqualTo("UNVERIFIED");
        var rows=List.of(row(first,number()));var valid=importer.dryRun(rows,first,"Synthetic source");
        var changed=List.of(new MemberImportRow(first,number(),true,"another-source"));
        assertThatThrownBy(() -> importer.apply(changed,first,"Synthetic source",valid.dryRunToken())).isInstanceOf(MemberRegistryException.class);
        assertThat(query.byIdentityUserId(first)).isEmpty();
    }
    @Autowired protected com.jeepclub.backend.iam.identity.core.application.service.user.UserExportService userExport;
    @Test void hundredMemberExportResolvesBusinessNumbersOncePerChunk() {
        String name="Synthetic export fixture "+first;
        for(int i=0;i<100;i++) {Long id=identity();jdbc.update("update identity_users set name=? where id=?",name,id);admin.assign(id,"EXPORT-BATCH-"+id,id,"Synthetic source");}
        var filter=new com.jeepclub.backend.iam.identity.core.application.query.user.AdminUserFilter(null,name,null,null,null,null,null,null,null,null,null,null,null);
        var context=org.springframework.security.core.context.SecurityContextHolder.getContext();var previous=context.getAuthentication();
        context.setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                new com.jeepclub.backend.platform.security.principal.UserPrincipal(first,999L,"Synthetic export",Instant.now().plusSeconds(3600)),null,List.of()));
        clearInvocations(projection);
        try {
            var file=userExport.export(filter,com.jeepclub.backend.shared.export.ExportFormat.CSV);
            assertThat(new String(file.bytes(),java.nio.charset.StandardCharsets.UTF_8).lines().count()).isEqualTo(101);
            verify(projection,times(1)).currentNumbers(argThat(ids -> ids.size()==100));
            verify(projection,never()).currentNumber(any());
        } finally {context.setAuthentication(previous);}
    }
    private MemberImportRow row(Long id,String number) { return new MemberImportRow(id,number,true,"synthetic-source-verified"); }
    private String attempt(Runnable action) { try { action.run();return "SUCCESS"; } catch(MemberRegistryException conflict) { return conflict.getKind().name(); } }
    private <T> List<T> race(Callable<T> a,Callable<T> b) throws Exception {
        var executor=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var go=new CountDownLatch(1);
        try {
            Callable<T> one=() -> {ready.countDown();go.await();return a.call();};Callable<T> two=() -> {ready.countDown();go.await();return b.call();};
            var x=executor.submit(one);var y=executor.submit(two);assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue();go.countDown();
            return List.of(x.get(30,TimeUnit.SECONDS),y.get(30,TimeUnit.SECONDS));
        } finally { go.countDown();executor.shutdownNow(); }
    }
}
