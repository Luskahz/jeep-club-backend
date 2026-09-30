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
    @BeforeEach void fixture() {
        registration=new EventRegistration(1L,1L,10L,EventRegistration.Status.CONFIRMED,List.of(new EventRegistration.Allocation(101L,true,List.of(201L)),new EventRegistration.Allocation(102L,false,List.of(202L))),now,now,null,List.of(203L));
        var event=Event.reconstitute(1L,99L,"Trilha de teste","Descrição",PublicationStatus.PUBLISHED,now,now,now,null,List.of(new PublicationImage("images/2026/09/27/550e8400-e29b-41d4-a716-446655440000.jpg",0,true)),now.plusSeconds(3600));
        when(publications.findById(1L)).thenReturn(Optional.of(event));
        var states=List.of(new EventFinancialQuery.State(50L,10L,1000L,"PENDING","PENDING_VALIDATION",now,"Taxa do evento",new BigDecimal("100.00"),70L,LocalDate.of(2026,10,1)));
        when(admin.dashboard(1L)).thenReturn(new AdminEventService.Dashboard(List.of(registration),3,1,1,5,2,7,2,1,1,1,states));
        when(users.findByIds(any())).thenReturn(List.of(new UserDetails(10L,"Titular A",null,null,"52998224725",null,null,null,true,now,null,null)));
        when(dependents.findDetailsByIds(any())).thenReturn(List.of(new DependentsQuery.Details(201L,10L,"Dependente A","1","CHILD","ACTIVE"),new DependentsQuery.Details(202L,10L,"Dependente B","2","CHILD","ACTIVE"),new DependentsQuery.Details(203L,10L,"Dependente C","3","CHILD","ACTIVE")));
        when(vehicles.findDetailsBatch(any())).thenReturn(List.of(new EventVehicleQuery.Details(101L,10L,"Jeep A","ABC1D23","Jeep","A",3),new EventVehicleQuery.Details(102L,10L,"Jeep B","DEF4G56","Jeep","B",4)));
        when(operations.guests(1L)).thenReturn(List.of(new EventGuestRequest(90L,1L,10L,102L,"12345678901",EventGuestRequest.Status.APPROVED,false,99L,now,now,null,"Convidado aprovado"),new EventGuestRequest(91L,1L,10L,null,"12345678902",EventGuestRequest.Status.PENDING,true,null,now,null,null,"Convidado pendente")));
        when(operations.rules(1L)).thenReturn(List.of(new EventChargeRule(1L,50L,true,now.minusSeconds(1),LocalDate.of(2026,10,1))));
        when(finance.definitions(any())).thenReturn(List.of(new EventFinanceReportQuery.Definition(50L,"Taxa do evento",new BigDecimal("100.00"))));
        when(finance.paymentCounts(1L)).thenReturn(Map.of("CONFIRMED",2L,"REJECTED",1L));
    }
    String report(EventReportService.Product product,EventReportService.TransportView view){return new String(service.export(1L,product,ExportFormat.CSV,null,null,null,view).bytes(),StandardCharsets.UTF_8);}
    @Test void multiVehicleManifestDoesNotDuplicateMemberOrMisallocateDependents() {
        String csv=report(EventReportService.Product.MANIFEST,EventReportService.TransportView.ALL);
        var lines=csv.lines().toList();assertThat(lines).hasSize(7);
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
    @Test void transportCountsOccupantsCapacityAndFreeSeats(){String csv=report(EventReportService.Product.TRANSPORT,EventReportService.TransportView.ALL);assertThat(csv.lines().count()).isEqualTo(3);assertThat(csv).contains("Convidado aprovado","Dependente B","Lugares livres").doesNotContain("Convidado pendente");}
    @Test void transportPendingViewsExposePendingGuestWithoutAllocating(){String csv=report(EventReportService.Product.TRANSPORT,EventReportService.TransportView.PENDING_GUESTS);assertThat(csv).contains("Convidado pendente","Não alocado").doesNotContain("Convidado aprovado");}
    @Test void acceptedRideNotSelectedIsReported(){when(operations.offers(1L)).thenReturn(List.of(new EventRideOffer(5L,1L,91L,1L,10L,101L,EventRideOffer.Status.ACCEPTED,now,null)));assertThat(report(EventReportService.Product.TRANSPORT,EventReportService.TransportView.ACCEPTED_RIDES)).contains("Carona aceita, ainda não selecionada");}
    @Test void accessIsMinimizedAndOmitsFinancialAndClinicalData(){String csv=report(EventReportService.Product.ACCESS,EventReportService.TransportView.ALL);assertThat(csv).contains("Convidado aprovado","Confirmação").doesNotContain("PENDING_VALIDATION","1000","Alergias","Situação financeira");verifyNoInteractions(coverage);}
    @Test void confirmedRegistrationCanHavePendingValidationAndPostCutoffDebt(){String csv=report(EventReportService.Product.FINANCIAL,EventReportService.TransportView.ALL);assertThat(csv).contains("CONFIRMED","PENDING_VALIDATION","Confirmado com pendência financeira","Taxa do evento","100,00");}
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
        verify(vehicles,never()).findDetailsBatch(any());
        verify(dependents,never()).findDetailsByIds(any());
    }
    @Test void healthCoverageUsesOnlyExistenceContract(){when(coverage.findCovered(eq(MedicalProfileOwner.USER),any())).thenReturn(Set.of(10L));String csv=report(EventReportService.Product.HEALTH_COVERAGE,EventReportService.TransportView.ALL);assertThat(csv.lines().count()).isEqualTo(5);assertThat(csv).contains("Possui ficha médica","Sim","Não").doesNotContain("Convidado","52998224725","PENDING_VALIDATION");}
    @Test void postEventUsesAllPaymentStatesAndSharedPeople(){String csv=report(EventReportService.Product.POST_EVENT,EventReportService.TransportView.ALL);assertThat(csv).contains("Participantes","Transporte","CONFIRMED","REJECTED","PENDING_VALIDATION","Pendências");}
    @Test void postEventPreservesRulesEvenWithoutGeneratedCharges(){
        when(admin.dashboard(1L)).thenReturn(new AdminEventService.Dashboard(List.of(),0,0,0,0,0,0,0,0,0,0,List.of()));
        assertThat(report(EventReportService.Product.POST_EVENT,EventReportService.TransportView.ALL)).contains("Regras","Obrigatória para participação: Sim","50","01/10/2026");
    }
    @Test void oversizedEventFailsBeforeLoadingDashboard(){doThrow(new ExportException(ExportException.Reason.LIMIT)).when(volume).requireWithinLimit(1L);assertThatThrownBy(()->report(EventReportService.Product.MANIFEST,EventReportService.TransportView.ALL)).isInstanceOf(ExportException.class);verifyNoInteractions(admin);}
    @Test void invalidFilterDoesNotQueryData(){assertThatThrownBy(()->service.export(1L,EventReportService.Product.MANIFEST,ExportFormat.CSV,"INVALID",null,null,EventReportService.TransportView.ALL)).isInstanceOf(ExportException.class);verifyNoInteractions(publications);}
    @Test void pdfIncludesOperationalDashboardAndGroups()throws Exception{var file=service.export(1L,EventReportService.Product.MANIFEST,ExportFormat.PDF,null,null,null,EventReportService.TransportView.ALL);try(var pdf=org.apache.pdfbox.Loader.loadPDF(file.bytes())){assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf)).contains("Inscrições: 1", "Titular A / Jeep A", "aguardando", "Página");}}
}
