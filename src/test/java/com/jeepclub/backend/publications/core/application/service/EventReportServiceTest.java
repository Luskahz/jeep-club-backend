package com.jeepclub.backend.publications.core.application.service;
import com.jeepclub.backend.publications.core.application.query.EventReportPeople;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.domain.enums.*;
import com.jeepclub.backend.publications.core.repository.*;
import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.vehicles.api.module.EventVehicleQuery;
import com.jeepclub.backend.health.api.module.medicalprofile.*;
import com.jeepclub.backend.billing.api.module.*;
import com.jeepclub.backend.shared.export.*;
import com.jeepclub.backend.platform.export.DefaultExportRenderer;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class EventReportServiceTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "MANIFEST,INVALID,,,ALL", "MANIFEST,,INVALID,,ALL",
        "MANIFEST,,,,FULL", "ACCESS,,,,AVAILABLE",
        "FINANCIAL,,MEMBER,,ALL", "FINANCIAL,,,true,ALL",
        "POST_EVENT,CONFIRMED,,,ALL", "POST_EVENT,,MEMBER,,ALL",
        "POST_EVENT,,,false,ALL", "HEALTH_COVERAGE,,,,PENDING_GUESTS"
    })
    void incompatibleFiltersFailBeforeAnyBoundaryCall(EventReportService.Product product, String status,
            String type, Boolean withVehicle, EventReportService.TransportView view) {
        clearInvocations(publications,admin,operations,volume,users,dependents,vehicles,coverage,finance,history);
        assertThatThrownBy(() -> service.export(1L,product,ExportFormat.CSV,status,type,withVehicle,view))
                .isInstanceOf(ExportException.class)
                .extracting("reason").isEqualTo(ExportException.Reason.INVALID_FILTER);
        verifyNoInteractions(publications,admin,operations,volume,users,dependents,vehicles,coverage,finance,history);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value=EventReportService.TransportView.class,names={"FULL","AVAILABLE"})
    void transportViewsUseExactRemainingCapacity(EventReportService.TransportView view) {
        doReturn(List.of(
                new EventVehicleQuery.Details(101L,10L,"Full vehicle","AAA1A11","Jeep","A",2),
                new EventVehicleQuery.Details(102L,10L,"Available vehicle","BBB2B22","Jeep","B",4))).when(vehicles).findDetailsBatch(any());
        var csv=report(EventReportService.Product.TRANSPORT,view);
        assertThat(csv.lines()).hasSize(2);
        if(view==EventReportService.TransportView.FULL) assertThat(csv).contains("Full vehicle").doesNotContain("Available vehicle","Capacidade excedida");
        else assertThat(csv).contains("Available vehicle").doesNotContain("Full vehicle");
        var filtered=new String(service.export(1L,EventReportService.Product.TRANSPORT,ExportFormat.CSV,
                null,"MEMBER",null,view).bytes(),StandardCharsets.UTF_8);
        assertThat(filtered.lines()).hasSize(view==EventReportService.TransportView.FULL?2:1);
        if(view==EventReportService.TransportView.FULL)assertThat(filtered).contains("Dependente A");
    }
    @Test void missingFinancialBoundaryDataIsExplicitAndCannotBeReportedAsSatisfied() {
        doReturn(List.of()).when(users).findByIds(any());
        when(finance.definitions(any())).thenReturn(List.of());
        when(admin.dashboard(1L)).thenReturn(new AdminEventService.Dashboard(List.of(registration),0,0,0,1,0,0,0,0,0,0,List.of()));
        var csv=report(EventReportService.Product.FINANCIAL,EventReportService.TransportView.ALL);
        assertThat(csv).contains("Cadastro indisponível","Cobrança ausente","Confirmado com pendência financeira","\"50\"")
                .doesNotContain("Obrigação satisfeita","Sem pendência financeira");
    }
    @Test void reportsBatchAllPublicBoundariesAt500AndKeepEveryParticipant() {
        var registrations=java.util.stream.LongStream.rangeClosed(1,501).mapToObj(id ->
                new EventRegistration(id,1L,id,EventRegistration.Status.CONFIRMED,
                        List.of(new EventRegistration.Allocation(1000+id,true,List.of(2000+id))),now,now,null)).toList();
        when(admin.dashboard(1L)).thenReturn(new AdminEventService.Dashboard(registrations,501,0,0,1002,501,1503,501,0,0,0,List.of()));
        when(operations.guests(1L)).thenReturn(List.of());
        doAnswer(call -> ((Collection<Long>)call.getArgument(0)).stream()
                .map(id -> new UserDetails(id,"Batch user "+id,null,null,null,null,null,null,true,now,null,null)).toList()).when(users).findByIds(any());
        doAnswer(call -> ((Collection<Long>)call.getArgument(0)).stream()
                .map(id -> new DependentsQuery.Details(id,id-2000,"Batch dependent "+id,null,"CHILD","ACTIVE")).toList()).when(dependents).findDetailsByIds(any());
        doAnswer(call -> ((Collection<Long>)call.getArgument(0)).stream()
                .map(id -> new EventVehicleQuery.Details(id,id-1000,"Batch vehicle "+id,"AAA1A11","Jeep","A",3)).toList()).when(vehicles).findDetailsBatch(any());
        when(coverage.findCovered(any(),any())).thenAnswer(call -> new HashSet<>((Collection<Long>)call.getArgument(1)));
        var csv=report(EventReportService.Product.HEALTH_COVERAGE,EventReportService.TransportView.ALL);
        assertThat(csv.lines()).hasSize(1003);
        assertThat(csv).contains("Batch user 501","Batch dependent 2501").doesNotContain("\"Não\"");
        var userBatches=org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(users,times(2)).findByIds(userBatches.capture());
        assertThat(userBatches.getAllValues()).extracting(Collection::size).containsExactlyInAnyOrder(500,1);
        assertThat(userBatches.getAllValues().stream().flatMap(Collection::stream).toList()).hasSize(501).doesNotHaveDuplicates();
        verify(dependents,times(2)).findDetailsByIds(argThat(ids -> ids.size()<=500));
        verify(vehicles,times(2)).findDetailsBatch(argThat(ids -> ids.size()<=500));
        verify(coverage,times(2)).findCovered(eq(MedicalProfileOwner.USER),argThat(ids -> ids.size()<=500));
        verify(coverage,times(2)).findCovered(eq(MedicalProfileOwner.DEPENDENT),argThat(ids -> ids.size()<=500));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(MedicalProfileOwner.class)
    void emergencyReportUsesAuthorizedProfileAndShowsMissingIdentityWithoutAnotherLookup(MedicalProfileOwner owner) throws Exception {
        var profile=new EmergencyMedicalProfileQuery.Profile("UNKNOWN","Synthetic allergy",null,null,null,null,null,null,null,null,null);
        when(admin.health(1L,owner,77L,99L)).thenReturn(profile);
        when(users.findById(77L)).thenReturn(Optional.empty());
        doReturn(List.of()).when(dependents).findDetailsByIds(List.of(77L));
        var file=service.emergency(1L,owner,77L,99L);
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(file.bytes())) {
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf).replaceAll("\\s+"," "))
                    .contains("Cadastro indisponível","Synthetic allergy","77");
        }
        verify(admin).health(1L,owner,77L,99L);
        if(owner==MedicalProfileOwner.USER) verify(users).findById(77L);
        else verify(dependents).findDetailsByIds(List.of(77L));
    }
    @Test void dependentEmergencyPdfUsesTheAuthorizedDependentsPublicIdentity() throws Exception {
        var profile=new EmergencyMedicalProfileQuery.Profile("UNKNOWN","Synthetic allergy",null,null,null,null,null,null,null,null,null);
        when(admin.health(1L,MedicalProfileOwner.DEPENDENT,77L,99L)).thenReturn(profile);
        doReturn(List.of(
                new DependentsQuery.Details(77L,10L,"Dependent emergency","00000000077","CHILD","ACTIVE"))).when(dependents).findDetailsByIds(List.of(77L));
        var file=service.emergency(1L,MedicalProfileOwner.DEPENDENT,77L,99L);
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(file.bytes())) {
            assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf).replaceAll("\\s+"," "))
                    .contains("Dependent emergency","00000000077","Synthetic allergy");
        }
        verify(users,never()).findById(any());
        verify(admin).health(1L,MedicalProfileOwner.DEPENDENT,77L,99L);
    }
    final Instant now=Instant.parse("2026-09-28T12:00:00Z");
    final AdminEventService admin=mock(AdminEventService.class);
    final PublicationRepository publications=mock(PublicationRepository.class);
    final EventPresentationQueryRepository history=mock(EventPresentationQueryRepository.class);
    final EventOperationsRepository operations=mock(EventOperationsRepository.class);
    final EventReportVolumeQuery volume=mock(EventReportVolumeQuery.class);
    final UserQuery users=mock(UserQuery.class);
    final DependentsQuery dependents=mock(DependentsQuery.class);
    final EventVehicleQuery vehicles=mock(EventVehicleQuery.class);
    final MedicalProfileCoverageQuery coverage=mock(MedicalProfileCoverageQuery.class);
    final EventFinanceReportQuery finance=mock(EventFinanceReportQuery.class);
    final Clock clock=Clock.fixed(now,ZoneOffset.UTC);
    final EventReportService service=new EventReportService(admin,publications,history,operations,volume,users,dependents,vehicles,coverage,finance,new DefaultExportRenderer(clock,20000,500,16777216),clock);
    EventRegistration registration;
    ExportDocument capturedDocument;
    @BeforeEach void fixture() {
        registration=new EventRegistration(1L,1L,10L,EventRegistration.Status.CONFIRMED,List.of(new EventRegistration.Allocation(101L,true,List.of(201L)),new EventRegistration.Allocation(102L,false,List.of(202L))),now,now,null,List.of(203L));
        var event=Event.reconstitute(1L,99L,"Trilha de teste","Descrição",PublicationStatus.PUBLISHED,now,now,now,null,List.of(new PublicationImage("images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg",0,true)),now.plusSeconds(3600));
        when(publications.findById(1L)).thenReturn(Optional.of(event));
        var states=List.of(new EventFinancialQuery.State(50L,10L,1000L,"PENDING","PENDING_VALIDATION",now,"Taxa do evento",new BigDecimal("100.00"),70L,LocalDate.of(2026,10,1)));
        when(admin.dashboard(1L)).thenReturn(new AdminEventService.Dashboard(List.of(registration),3,1,1,5,2,7,2,1,1,1,states));
        var knownUsers=List.of(new UserDetails(10L,"Titular A",null,null,"52998224725",null,null,null,true,now,null,null));
        var knownDependents=List.of(new DependentsQuery.Details(201L,10L,"Dependente A","1","CHILD","ACTIVE"),new DependentsQuery.Details(202L,10L,"Dependente B","2","CHILD","ACTIVE"),new DependentsQuery.Details(203L,10L,"Dependente C","3","CHILD","ACTIVE"));
        var knownVehicles=List.of(new EventVehicleQuery.Details(101L,10L,"Jeep A","ABC1D23","Jeep","A",3),new EventVehicleQuery.Details(102L,10L,"Jeep B","DEF4G56","Jeep","B",4));
        doAnswer(call -> knownUsers.stream().filter(u -> ((Collection<Long>)call.getArgument(0)).contains(u.id())).toList()).when(users).findByIds(any());
        doAnswer(call -> knownDependents.stream().filter(d -> ((Collection<Long>)call.getArgument(0)).contains(d.id())).toList()).when(dependents).findDetailsByIds(any());
        doAnswer(call -> knownVehicles.stream().filter(v -> ((Collection<Long>)call.getArgument(0)).contains(v.id())).toList()).when(vehicles).findDetailsBatch(any());
        when(operations.guests(1L)).thenReturn(List.of(new EventGuestRequest(90L,1L,10L,102L,"12345678901",EventGuestRequest.Status.APPROVED,false,99L,now,now,null,"Convidado aprovado",1L,102L,1L,10L,null),
            new EventGuestRequest(91L,1L,10L,null,"12345678902",EventGuestRequest.Status.PENDING,false,null,now,null,null,"Convidado pendente",1L,101L,null,null,null)));
        when(operations.rules(1L)).thenReturn(List.of(new EventChargeRule(1L,50L,true,now.minusSeconds(1),LocalDate.of(2026,10,1))));
        when(finance.definitions(any())).thenReturn(List.of(new EventFinanceReportQuery.Definition(50L,"Taxa do evento",new BigDecimal("100.00"))));
        when(finance.paymentCounts(1L)).thenReturn(Map.of("CONFIRMED",2L,"REJECTED",1L));
    }
    String report(EventReportService.Product product,EventReportService.TransportView view){return new String(service.export(1L,product,ExportFormat.CSV,null,null,null,view).bytes(),StandardCharsets.UTF_8);}
    List<Map<String,String>> semanticRows(EventReportService.Product product) {
        return semanticRows(product,null,null,null);
    }
    List<Map<String,String>> semanticRows(EventReportService.Product product,String status,String type,Boolean withVehicle) {
        return semanticRows(product,status,type,withVehicle,EventReportService.TransportView.ALL);
    }
    List<Map<String,String>> semanticRows(EventReportService.Product product,String status,String type,Boolean withVehicle,EventReportService.TransportView view) {
        var result=new ArrayList<Map<String,String>>();
        ExportRenderer renderer=(document,format) -> {
            capturedDocument=document;
            if(status!=null)assertThat(document.filters()).contains("Inscrição: "+status);
            if(type!=null)assertThat(document.filters()).contains("Tipo: "+type);
            if(withVehicle!=null)assertThat(document.filters()).contains("Com veículo: "+(withVehicle?"Sim":"Não"));
            document.rows().read(row -> {
                assertThat(row.cells()).hasSameSizeAs(document.columns());
                var values=new LinkedHashMap<String,String>();
                for(int i=0;i<document.columns().size();i++)values.put(document.columns().get(i),row.cells().get(i));
                result.add(values);
            });
            return new ExportFile("probe.csv","text/csv",new byte[0]);
        };
        new EventReportService(admin,publications,history,operations,volume,users,dependents,vehicles,coverage,finance,renderer,clock)
                .export(1L,product,ExportFormat.CSV,status,type,withVehicle,view);
        return result;
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        ",MEMBER,,Titular A",
        ",DEPENDENT,,Dependente A|Dependente B|Dependente C",
        ",GUEST,,Convidado aprovado|Convidado pendente",
        "PENDING,,,Convidado pendente",
        "CONFIRMED,,,Titular A|Dependente A|Dependente B|Dependente C|Convidado aprovado",
        "REJECTED,,,",
        ",,true,Titular A|Dependente A|Dependente B|Convidado aprovado",
        ",,false,Dependente C|Convidado pendente",
        "CONFIRMED,DEPENDENT,false,Dependente C",
        "PENDING,MEMBER,true,"
    })
    void validManifestFiltersSelectExactlyTheRequestedParticipants(String status,String type,Boolean withVehicle,String names) {
        var expected=names==null?List.<String>of():Arrays.asList(names.split("\\|"));
        assertThat(semanticRows(EventReportService.Product.MANIFEST,status,type,withVehicle).stream()
                .map(row -> row.get("Nome")).toList()).containsExactlyInAnyOrderElementsOf(expected);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"CONFIRMED,1","PENDING_PAYMENT,0"})
    void financialStatusFilterDoesNotReturnAnotherRegistrationState(String status,int count) {
        assertThat(semanticRows(EventReportService.Product.FINANCIAL,status,null,null)).hasSize(count);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"PENDING,,0",",false,0",",true,2"})
    void transportFiltersDoNotReturnVehiclesOutsideTheSelectedParticipants(String status,Boolean withVehicle,int count) {
        assertThat(semanticRows(EventReportService.Product.TRANSPORT,status,null,withVehicle)).hasSize(count);
    }
    @Test void manifestFinancialSummaryIsResolvedForEachHouseholdSeparately() {
        var other=new EventRegistration(2L,1L,20L,EventRegistration.Status.CONFIRMED,List.of(),now,now,null);
        var ownState=admin.dashboard(1L).financial().get(0);
        var otherState=new EventFinancialQuery.State(50L,20L,2000L,"PAID","CONFIRMED",now);
        when(admin.dashboard(1L)).thenReturn(new AdminEventService.Dashboard(List.of(registration,other),3,1,1,6,2,7,2,1,1,1,List.of(ownState,otherState)));
        var known=List.of(new UserDetails(10L,"Titular A",null,null,null,null,null,null,true,now,null,null),
                new UserDetails(20L,"Other payer",null,null,null,null,null,null,true,now,null,null));
        doAnswer(call -> known.stream().filter(u -> ((Collection<Long>)call.getArgument(0)).contains(u.id())).toList()).when(users).findByIds(any());
        var rows=semanticRows(EventReportService.Product.MANIFEST);
        assertThat(rows).filteredOn(row -> row.get("Titular").equals("Titular A")).hasSize(6)
                .allSatisfy(row -> assertThat(row).containsEntry("Situação financeira","PENDING / PENDING_VALIDATION"));
        assertThat(rows).filteredOn(row -> row.get("Nome").equals("Other payer")).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("Situação financeira","PAID / CONFIRMED"));
    }
    @Test void transportParticipantFilterKeepsPhysicalOccupantsInTheSelectedVehicle() {
        assertThat(semanticRows(EventReportService.Product.TRANSPORT,null,"MEMBER",null)).singleElement().satisfies(row ->
                assertThat(row).containsEntry("ID do veículo","101").containsEntry("Ocupação total","2")
                        .containsEntry("Lugares livres","1").containsEntry("Dependentes","Dependente A"));
    }
    @Test void guestOnlyReferencesResolveRequesterResponsibleAndCurrentVehicleOwnerIndependently() {
        var guests=List.of(
                new EventGuestRequest(94L,1L,30L,104L,"12345678903",EventGuestRequest.Status.APPROVED,false,
                        99L,now,now,null,"Personal sparse guest",2L,104L,2L,30L,null),
                new EventGuestRequest(95L,1L,99L,105L,"12345678904",EventGuestRequest.Status.APPROVED,true,
                        99L,now,now,null,"Administrative sparse guest",null,null,3L,40L,null));
        when(operations.guests(1L)).thenReturn(guests);
        var identities=List.of(
                new UserDetails(10L,"Titular A",null,null,null,null,null,null,true,now,null,null),
                new UserDetails(30L,"Requester",null,null,null,null,null,null,true,now,null,null),
                new UserDetails(40L,"Responsible",null,null,null,null,null,null,true,now,null,null),
                new UserDetails(50L,"Current owner",null,null,null,null,null,null,true,now,null,null));
        doAnswer(call -> identities.stream().filter(u -> ((Collection<Long>)call.getArgument(0)).contains(u.id())).toList())
                .when(users).findByIds(any());
        var details=List.of(new EventVehicleQuery.Details(104L,30L,"Guest vehicle","AAA1A11","Jeep","A",3),
                new EventVehicleQuery.Details(105L,50L,"Transferred vehicle","BBB2B22","Jeep","B",3));
        doAnswer(call -> details.stream().filter(v -> ((Collection<Long>)call.getArgument(0)).contains(v.id())).toList())
                .when(vehicles).findDetailsBatch(any());
        var manifest=semanticRows(EventReportService.Product.MANIFEST);
        assertThat(manifest).filteredOn(row -> row.get("Nome").equals("Personal sparse guest")).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("Titular","Requester").containsEntry("Placa","AAA1A11"));
        assertThat(manifest).filteredOn(row -> row.get("Nome").equals("Administrative sparse guest")).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("Titular","Responsible").containsEntry("Placa","BBB2B22"));
        assertThat(semanticRows(EventReportService.Product.TRANSPORT)).filteredOn(row -> row.get("ID do veículo").equals("105"))
                .singleElement().satisfies(row -> assertThat(row).containsEntry("Proprietário","Current owner"));
    }
    @Test void healthCoverageNeverQueriesAMismatchedHouseholdsDependent() {
        doReturn(List.of(
                new DependentsQuery.Details(201L,999L,"Other household",null,"CHILD","ACTIVE"),
                new DependentsQuery.Details(202L,10L,"Dependent B",null,"CHILD","ACTIVE"),
                new DependentsQuery.Details(203L,10L,"Dependent C",null,"CHILD","ACTIVE"))).when(dependents).findDetailsByIds(any());
        var rows=semanticRows(EventReportService.Product.HEALTH_COVERAGE);
        assertThat(rows).hasSize(3).allSatisfy(row -> assertThat(row.get("Participante")).isNotEqualTo("Other household"));
        verify(coverage).findCovered(eq(MedicalProfileOwner.DEPENDENT),argThat(ids ->
                ids.size()==2 && ids.containsAll(List.of(202L,203L)) && !ids.contains(201L)));
    }
    @Test void cancelledParticipantsHaveNoHealthCoverageLookup() {
        when(admin.dashboard(1L)).thenReturn(new AdminEventService.Dashboard(List.of(registration.cancel(now)),0,0,0,0,0,0,0,0,0,0,List.of()));
        assertThat(semanticRows(EventReportService.Product.HEALTH_COVERAGE)).isEmpty();
        verifyNoInteractions(coverage);
    }
    @Test void multiVehicleManifestDoesNotDuplicateMemberOrMisallocateDependents() {
        String csv=report(EventReportService.Product.MANIFEST,EventReportService.TransportView.ALL);
        var lines=csv.lines().toList();assertThat(lines).hasSize(7);
        assertThat(lines.get(1)).contains("\"Membro\"");
        assertThat(lines.stream().filter(l->l.contains("\"Membro\"")).count()).isEqualTo(1);
        assertThat(lines.stream().filter(l->l.contains("Dependente A")).findFirst().orElseThrow()).contains("Jeep A").doesNotContain("Jeep B");
        assertThat(lines.stream().filter(l->l.contains("Dependente B")).findFirst().orElseThrow()).contains("Jeep B");
        assertThat(lines.stream().filter(l->l.contains("Dependente C")).findFirst().orElseThrow()).contains("Não alocado");
        assertThat(lines.stream().filter(l->l.contains("Convidado aprovado")).findFirst().orElseThrow()).contains("Jeep B");
    }
    @Test void oneVehicleProducesOneMemberLine(){
        var single=new EventRegistration(2L,1L,10L,EventRegistration.Status.CONFIRMED,List.of(new EventRegistration.Allocation(101L,true,List.of())),now,now,null);
        var people=EventReportPeople.assemble(List.of(single),List.of(),Map.of(),Map.of());
        assertThat(people).hasSize(1);assertThat(people.get(0).vehicleId()).isEqualTo(101L);
    }
    @Test void transportCountsOccupantsCapacityAndFreeSeats() {
        String csv=report(EventReportService.Product.TRANSPORT,EventReportService.TransportView.ALL);
        assertThat(csv.lines().count()).isEqualTo(3);
        assertThat(csv).contains("Convidado aprovado","Dependente B","Lugares livres").doesNotContain("Convidado pendente");
        var rows=semanticRows(EventReportService.Product.TRANSPORT);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("ID do veículo","101").containsEntry("Capacidade","3")
                .containsEntry("Ocupação total","2").containsEntry("Lugares livres","1")
                .containsEntry("Dependentes","Dependente A").containsEntry("Convidados","")
                .containsEntry("Proprietário","Titular A").containsEntry("CPF do proprietário","52998224725");
        assertThat(rows.get(1)).containsEntry("ID do veículo","102").containsEntry("Capacidade","4")
                .containsEntry("Ocupação total","2").containsEntry("Lugares livres","2")
                .containsEntry("Dependentes","Dependente B").containsEntry("Convidados","Convidado aprovado");
    }
    @Test void transportPendingViewsExposePendingGuestWithoutAllocating(){String csv=report(EventReportService.Product.TRANSPORT,EventReportService.TransportView.PENDING_GUESTS);assertThat(csv).contains("Convidado pendente","Não alocado").doesNotContain("Convidado aprovado");}
    @Test void standbyReportKeepsDuplicateCpfParticipationsSeparateAndExcludesPendingPersonalGuest() {
        var pending=EventGuestRequest.requested(1L,10L,1L,101L,"12345678902","Pending personal",now).identified(91L);
        var first=EventGuestRequest.standby(1L,99L,"12345678902","Standby first",now).identified(92L);
        var second=EventGuestRequest.standby(1L,99L,"12345678902","Standby second",now).identified(93L);
        var allocated=operations.guests(1L).get(0);
        when(operations.guests(1L)).thenReturn(List.of(pending,first,second,allocated));
        String csv=report(EventReportService.Product.TRANSPORT,EventReportService.TransportView.UNALLOCATED_GUESTS);
        assertThat(csv).contains("Standby first","Standby second").doesNotContain("Pending personal","Convidado aprovado");
        assertThat(csv.lines().filter(line -> line.contains("Standby first")||line.contains("Standby second")).count()).isEqualTo(2);
        assertThat(report(EventReportService.Product.TRANSPORT,EventReportService.TransportView.UNALLOCATED_PEOPLE))
                .contains("Dependente C","Standby first","Standby second").doesNotContain("Pending personal");
        var rows=semanticRows(EventReportService.Product.TRANSPORT,null,null,null,EventReportService.TransportView.UNALLOCATED_PEOPLE);
        assertThat(rows).filteredOn(row -> row.get("Ocupantes").equals("Dependente C")).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("Dependentes","Dependente C").containsEntry("Convidados",""));
        assertThat(rows).filteredOn(row -> row.get("Ocupantes").equals("Standby first")).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("Dependentes","").containsEntry("Convidados","Standby first"));
    }
    @Test void acceptedRideNotSelectedIsReported(){when(operations.offers(1L)).thenReturn(List.of(new EventRideOffer(5L,1L,91L,1L,10L,101L,EventRideOffer.Status.ACCEPTED,now,null)));assertThat(report(EventReportService.Product.TRANSPORT,EventReportService.TransportView.ACCEPTED_RIDES)).contains("Carona aceita, ainda não selecionada","Titular A","Jeep A","ABC1D23","\"3\"");}
    @Test void accessIsMinimizedAndOmitsFinancialAndClinicalData(){String csv=report(EventReportService.Product.ACCESS,EventReportService.TransportView.ALL);assertThat(csv).contains("Convidado aprovado","Confirmação").doesNotContain("PENDING_VALIDATION","1000","Alergias","Situação financeira");verifyNoInteractions(coverage);}
    @Test void confirmedRegistrationCanHavePendingValidationAndPostCutoffDebt() {
        var state=new EventFinancialQuery.State(50L,10L,1000L,"PENDING","PENDING_VALIDATION",now,
                "Taxa do evento",new BigDecimal("100.00"),70L,LocalDate.of(2026,10,2));
        when(admin.dashboard(1L)).thenReturn(new AdminEventService.Dashboard(List.of(registration),3,1,1,5,2,7,2,1,1,1,List.of(state)));
        when(finance.definitions(any())).thenReturn(List.of(new EventFinanceReportQuery.Definition(50L,"Changed catalog",new BigDecimal("999.00"))));
        String csv=report(EventReportService.Product.FINANCIAL,EventReportService.TransportView.ALL);
        assertThat(csv).contains("CONFIRMED","PENDING_VALIDATION","Confirmado com pendência financeira","Taxa do evento","100,00")
                .doesNotContain("Changed catalog","999,00");
        assertThat(semanticRows(EventReportService.Product.FINANCIAL)).singleElement().satisfies(row ->
                assertThat(row).containsEntry("ID do usuário","10").containsEntry("Nome","Titular A")
                        .containsEntry("CPF","52998224725").containsEntry("ID da inscrição","1")
                        .containsEntry("Situação da inscrição","CONFIRMED").containsEntry("Definição","Taxa do evento")
                        .containsEntry("Valor","100,00").containsEntry("Obrigatória para participação","Sim")
                        .containsEntry("Vencimento financeiro","02/10/2026").containsEntry("ID da cobrança","1000")
                        .containsEntry("Situação efetiva da cobrança","PENDING").containsEntry("Situação do pagamento","PENDING_VALIDATION")
                        .containsEntry("Aguardando validação","Sim").containsEntry("Situação para participação","Confirmado com pendência financeira"));
    }
    @Test void reducedVehicleCapacityIsReportedWithoutNegativeFreeSeatsOrHiddenOccupants() {
        doReturn(List.of(
                new EventVehicleQuery.Details(101L,10L,"Reduced","AAA1A11","Jeep","A",1),
                new EventVehicleQuery.Details(102L,10L,"Other","BBB2B22","Jeep","B",4))).when(vehicles).findDetailsBatch(any());
        assertThat(semanticRows(EventReportService.Product.TRANSPORT)).filteredOn(row -> row.get("ID do veículo").equals("101"))
                .singleElement().satisfies(row ->
                assertThat(row).containsEntry("Ocupação total","2").containsEntry("Lugares livres","0")
                        .containsEntry("Pendência","Capacidade excedida após alteração do veículo"));
    }
    @Test void financialVolumeLimitFailsBeforeLoadingTheOperationalDashboard() {
        doThrow(new ExportException(ExportException.Reason.LIMIT)).when(finance).requireWithinLimit(1L);
        assertThatThrownBy(() -> report(EventReportService.Product.FINANCIAL,EventReportService.TransportView.ALL))
                .isInstanceOfSatisfying(ExportException.class,error -> assertThat(error.reason()).isEqualTo(ExportException.Reason.LIMIT));
        verifyNoInteractions(admin);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "PAID,CONFIRMED,-1,Obrigação satisfeita",
        "CANCELED,CANCELED,-1,Confirmado com pendência financeira",
        "PENDING,PENDING_VALIDATION,-1,Obrigação satisfeita; aguardando validação",
        "PENDING,PENDING_VALIDATION,0,Obrigação satisfeita; aguardando validação",
        "PENDING,PENDING_VALIDATION,1,Confirmado com pendência financeira",
        "PAID,CONFIRMED,1,Confirmado com pendência financeira",
        "PENDING,REJECTED,-1,Confirmado com pendência financeira",
        "OVERDUE,REJECTED,-1,Confirmado com pendência financeira",
        "EXPIRED,REJECTED,-1,Confirmado com pendência financeira"
    })
    void participationUsesConfirmationRuleAndKeepsAllStatuses(String charge,String payment,long submittedOffset,String expected) {
        when(operations.rules(1L)).thenReturn(List.of(new EventChargeRule(1L,50L,true,now,LocalDate.of(2026,10,1))));
        var state=new EventFinancialQuery.State(50L,10L,1000L,charge,payment,now.plusSeconds(submittedOffset),"Taxa",BigDecimal.TEN,70L,LocalDate.of(2026,10,1));
        when(admin.dashboard(1L)).thenReturn(new AdminEventService.Dashboard(List.of(registration),0,0,0,1,0,0,0,0,0,0,List.of(state)));
        var csv=report(EventReportService.Product.FINANCIAL,EventReportService.TransportView.ALL);
        assertThat(csv).contains("Situação da inscrição","Situação efetiva da cobrança","Situação do pagamento","\"CONFIRMED\"","\""+charge+"\"","\""+payment+"\"",expected);
        if(charge.equals("CANCELED"))assertThat(csv).doesNotContain("Obrigação satisfeita","Sem pendência financeira");
    }
    @Test void missingPaymentSubmissionDoesNotSatisfyParticipation() {
        var state=new EventFinancialQuery.State(50L,10L,1000L,"PAID","CONFIRMED",null);
        when(admin.dashboard(1L)).thenReturn(new AdminEventService.Dashboard(List.of(registration),0,0,0,1,0,0,0,0,0,0,List.of(state)));
        assertThat(report(EventReportService.Product.FINANCIAL,EventReportService.TransportView.ALL)).contains("Confirmado com pendência financeira");
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "PENDING_PAYMENT,CANCELED,true,1,Falta pagar",
        "PENDING_PAYMENT,OVERDUE,true,-1,Passou do limite de participação",
        "PENDING_PAYMENT,EXPIRED,true,-1,Passou do limite de participação",
        "PENDING_PAYMENT,CANCELED,false,-1,Não obrigatória para participação",
        "CANCELLED,PAID,true,-1,Inscrição cancelada"
    })
    void participationDistinguishesCutoffOptionalChargeAndCancellation(String status,String charge,boolean required,long cutoffOffset,String expected) {
        var r=new EventRegistration(1L,1L,10L,EventRegistration.Status.valueOf(status),List.of(),now,null,null);
        var state=new EventFinancialQuery.State(50L,10L,1000L,charge,"CANCELED",now);
        when(operations.rules(1L)).thenReturn(List.of(new EventChargeRule(1L,50L,required,now.plusSeconds(cutoffOffset),LocalDate.of(2026,10,1))));
        when(admin.dashboard(1L)).thenReturn(new AdminEventService.Dashboard(List.of(r),0,0,0,0,0,0,0,0,0,0,List.of(state)));
        assertThat(report(EventReportService.Product.FINANCIAL,EventReportService.TransportView.ALL)).contains(expected).doesNotContain("Sem pendência financeira");
    }
    @Test void historicalFallbackIsLimitedToPostEventAndDoesNotRefreshRegistration() {
        var dashboard=admin.dashboard(1L);
        clearInvocations(admin);
        when(publications.findById(1L)).thenReturn(Optional.empty());
        when(history.findHistoricalById(1L)).thenReturn(Optional.of(new EventPresentationQueryRepository.HistoricalEvent(1L,"Trilha excluída",now.minusSeconds(3600),now,EventStatus.FINISHED,now)));
        when(admin.historicalDashboard(1L)).thenReturn(dashboard);
        assertThat(report(EventReportService.Product.POST_EVENT,EventReportService.TransportView.ALL)).contains("Trilha excluída","Histórico","FINISHED","Titular A","PENDING_VALIDATION");
        verify(admin,never()).dashboard(anyLong());
        for(var product:EventReportService.Product.values())if(product!=EventReportService.Product.POST_EVENT)
            assertThatThrownBy(()->report(product,EventReportService.TransportView.ALL)).isInstanceOf(ExportException.class);
    }
    @Test void historicalPresentationNeverRevealsAnotherHouseholdAndKeepsMissingVehicleId() {
        when(publications.findById(1L)).thenReturn(Optional.empty());
        when(history.findHistoricalById(1L)).thenReturn(Optional.of(new EventPresentationQueryRepository.HistoricalEvent(
            1L,"Trilha excluída",now.minusSeconds(3600),now,EventStatus.FINISHED,now)));
        var dashboard=admin.dashboard(1L);
        when(admin.historicalDashboard(1L)).thenReturn(dashboard);
        when(vehicles.findPresentationDetailsBatch(any())).thenReturn(List.of(
            new EventVehicleQuery.Details(101L,10L,"Jeep histórico","ABC1D23","Jeep","A",3)));
        when(dependents.findPresentationDetailsByIds(any())).thenReturn(List.of(
            new DependentsQuery.Details(201L,10L,"Dependente legítimo","1","CHILD","ACTIVE"),
            new DependentsQuery.Details(202L,999L,"NOME DE OUTRA FAMÍLIA","2","CHILD","ACTIVE")));
        String post=report(EventReportService.Product.POST_EVENT,EventReportService.TransportView.ALL);
        assertThat(post).contains("Dependente legítimo","Jeep histórico","Veículo indisponível (ID 102)",
            "Cadastro do veículo indisponível").doesNotContain("NOME DE OUTRA FAMÍLIA");
        assertThat(semanticRows(EventReportService.Product.POST_EVENT))
                .filteredOn(row -> row.get("Seção").equals("Transporte") && row.get("Identificação").equals("102"))
                .singleElement().satisfies(row -> assertThat(row).containsEntry("Ocupação","2").containsEntry("Vagas",""));
        verify(vehicles,never()).findDetailsBatch(any());
        verify(dependents,never()).findDetailsByIds(any());
    }
    @Test void healthCoverageUsesOnlyExistenceContract() {
        when(coverage.findCovered(eq(MedicalProfileOwner.USER),any())).thenReturn(Set.of(10L));
        String csv=report(EventReportService.Product.HEALTH_COVERAGE,EventReportService.TransportView.ALL);
        assertThat(csv.lines().count()).isEqualTo(5);
        assertThat(csv).contains("Possui ficha médica","Sim","Não").doesNotContain("Convidado","52998224725","PENDING_VALIDATION");
        var rows=semanticRows(EventReportService.Product.HEALTH_COVERAGE);
        assertThat(rows).filteredOn(row -> row.get("Participante").equals("Titular A")).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("Tipo","USER").containsEntry("Possui ficha médica","Sim"));
        assertThat(rows).filteredOn(row -> !row.get("Participante").equals("Titular A")).hasSize(3)
                .allSatisfy(row -> assertThat(row).containsEntry("Tipo","DEPENDENT").containsEntry("Possui ficha médica","Não"));
    }
    @Test void postEventUsesAllPaymentStatesAndSharedPeople() {
        var cancelled=new EventRegistration(2L,1L,20L,EventRegistration.Status.CONFIRMED,
                List.of(new EventRegistration.Allocation(103L,true,List.of())),now,now,null,List.of(204L)).cancel(now);
        var states=admin.dashboard(1L).financial();
        var pending=new EventRegistration(3L,1L,30L,EventRegistration.Status.PENDING_PAYMENT,List.of(),now,null,null);
        when(admin.dashboard(1L)).thenReturn(new AdminEventService.Dashboard(List.of(registration,cancelled,pending),3,1,1,5,2,7,2,1,1,1,states));
        var people=List.of(new UserDetails(10L,"Titular A",null,null,null,null,null,null,true,now,null,null),
                new UserDetails(20L,"Cancelled member",null,null,null,null,null,null,true,now,null,null));
        doAnswer(call -> people.stream().filter(u -> ((Collection<Long>)call.getArgument(0)).contains(u.id())).toList()).when(users).findByIds(any());
        var vehiclesData=List.of(new EventVehicleQuery.Details(101L,10L,"Jeep A","ABC1D23","Jeep","A",3),
                new EventVehicleQuery.Details(102L,10L,"Jeep B","DEF4G56","Jeep","B",4),
                new EventVehicleQuery.Details(103L,20L,"Cancelled van","GHI7J89","Jeep","C",2));
        doAnswer(call -> vehiclesData.stream().filter(v -> ((Collection<Long>)call.getArgument(0)).contains(v.id())).toList()).when(vehicles).findDetailsBatch(any());
        var deps=List.of(new DependentsQuery.Details(201L,10L,"Dependente A","1","CHILD","ACTIVE"),
                new DependentsQuery.Details(202L,10L,"Dependente B","2","CHILD","ACTIVE"),
                new DependentsQuery.Details(203L,10L,"Dependente C","3","CHILD","ACTIVE"),
                new DependentsQuery.Details(204L,20L,"Cancelled dependent","4","CHILD","ACTIVE"));
        doAnswer(call -> deps.stream().filter(d -> ((Collection<Long>)call.getArgument(0)).contains(d.id())).toList()).when(dependents).findDetailsByIds(any());
        String csv=report(EventReportService.Product.POST_EVENT,EventReportService.TransportView.ALL);
        assertThat(csv).contains("Participantes","Transporte","CONFIRMED","REJECTED","PENDING_VALIDATION","Pendências");
        var transport=semanticRows(EventReportService.Product.POST_EVENT).stream().filter(row -> row.get("Seção").equals("Transporte")).toList();
        assertThat(transport).hasSize(3);
        assertThat(transport).filteredOn(row -> row.get("Identificação").equals("101")).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("Ocupação","2").containsEntry("Vagas","1"));
        assertThat(transport).filteredOn(row -> row.get("Identificação").equals("102")).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("Ocupação","2").containsEntry("Vagas","2"));
        assertThat(transport).filteredOn(row -> row.get("Identificação").equals("103")).singleElement()
                .satisfies(row -> assertThat(row).containsEntry("Ocupação","0").containsEntry("Vagas","2"));
        assertThat(capturedDocument.filters()).contains("Inscrições: 3; confirmados: 1; cancelados: 1");
        assertThat(report(EventReportService.Product.TRANSPORT,EventReportService.TransportView.UNALLOCATED_PEOPLE))
                .contains("Dependente C").doesNotContain("Cancelled dependent");
    }
    @Test void postEventPreservesRulesEvenWithoutGeneratedCharges(){
        when(admin.dashboard(1L)).thenReturn(new AdminEventService.Dashboard(List.of(),0,0,0,0,0,0,0,0,0,0,List.of()));
        assertThat(report(EventReportService.Product.POST_EVENT,EventReportService.TransportView.ALL)).contains("Regras","Obrigatória para participação: Sim","50","01/10/2026");
    }
    @Test void oversizedEventFailsBeforeLoadingDashboard(){doThrow(new ExportException(ExportException.Reason.LIMIT)).when(volume).requireWithinLimit(1L);assertThatThrownBy(()->report(EventReportService.Product.MANIFEST,EventReportService.TransportView.ALL)).isInstanceOf(ExportException.class);verifyNoInteractions(admin);}
    @Test void invalidFilterDoesNotQueryData(){assertThatThrownBy(()->service.export(1L,EventReportService.Product.MANIFEST,ExportFormat.CSV,"INVALID",null,null,EventReportService.TransportView.ALL)).isInstanceOf(ExportException.class);verifyNoInteractions(publications);}
    @Test void pdfIncludesOperationalDashboardAndGroups()throws Exception{var file=service.export(1L,EventReportService.Product.MANIFEST,ExportFormat.PDF,null,null,null,EventReportService.TransportView.ALL);try(var pdf=org.apache.pdfbox.Loader.loadPDF(file.bytes())){assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf)).contains("Manifesto de participantes", "Inscrições: 1", "Titular A / Jeep A", "aguardando", "Página");}}
}
