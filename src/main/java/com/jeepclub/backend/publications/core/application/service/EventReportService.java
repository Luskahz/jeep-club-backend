package com.jeepclub.backend.publications.core.application.service;
import com.jeepclub.backend.publications.core.application.query.EventReportPeople;
import com.jeepclub.backend.publications.core.application.query.EventReportPeople.Person;
import com.jeepclub.backend.publications.core.application.service.internal.EventParticipationRequirement;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.repository.*;
import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.vehicles.api.module.EventVehicleQuery;
import com.jeepclub.backend.health.api.module.medicalprofile.*;
import com.jeepclub.backend.billing.api.module.*;
import com.jeepclub.backend.shared.export.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
@org.springframework.stereotype.Service @lombok.RequiredArgsConstructor
public class EventReportService {
    public enum Product { MANIFEST,TRANSPORT,FINANCIAL,ACCESS,HEALTH_COVERAGE,POST_EVENT }
    public enum TransportView { ALL,FULL,AVAILABLE,UNALLOCATED_PEOPLE,UNALLOCATED_GUESTS,PENDING_GUESTS,ACCEPTED_RIDES }
    private final AdminEventService administration;
    private final PublicationRepository publications;
    private final EventPresentationQueryRepository eventHistory;
    private final EventOperationsRepository operations;
    private final EventReportVolumeQuery volume;
    private final UserQuery users;
    private final DependentsQuery dependents;
    private final EventVehicleQuery vehicles;
    private final MedicalProfileCoverageQuery coverage;
    private final EventFinanceReportQuery finance;
    private final ExportRenderer renderer;
    private final Clock clock;
    @org.springframework.transaction.annotation.Transactional(isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public ExportFile export(Long eventId,Product product,ExportFormat format,String status,String type,Boolean withVehicle,TransportView view) {
        if(status!=null && !List.of("PENDING_PAYMENT","CONFIRMED","CANCELLED","PENDING","REJECTED").contains(status)
            || type!=null && !List.of("MEMBER","DEPENDENT","GUEST").contains(type))throw new ExportException(ExportException.Reason.INVALID_FILTER);
        if(product!=Product.TRANSPORT && view!=TransportView.ALL
            || (product==Product.FINANCIAL || product==Product.POST_EVENT) && (type!=null || withVehicle!=null)
            || product==Product.POST_EVENT && status!=null)throw new ExportException(ExportException.Reason.INVALID_FILTER);
        var event=publications.findById(eventId).filter(Event.class::isInstance).map(Event.class::cast).orElse(null);
        var historical=event==null && product==Product.POST_EVENT?eventHistory.findHistoricalById(eventId).orElse(null):null;
        if(event==null && historical==null)throw new ExportException(ExportException.Reason.NOT_FOUND);
        volume.requireWithinLimit(eventId);finance.requireWithinLimit(eventId);
        var dashboard=event==null?administration.historicalDashboard(eventId):administration.dashboard(eventId);
        var registrations=dashboard.registrations();var guests=operations.guests(eventId);var rules=operations.rules(eventId);
        var userIds=new HashSet<Long>();registrations.forEach(r->userIds.add(r.userId()));guests.forEach(g->userIds.add(g.requesterUserId()));
        var depIds=new HashSet<Long>();registrations.forEach(r->{depIds.addAll(r.unallocatedDependentIds());r.allocations().forEach(a->depIds.addAll(a.dependentIds()));});
        var vehicleIds=new HashSet<Long>();registrations.forEach(r->r.allocations().forEach(a->vehicleIds.add(a.vehicleId())));guests.stream().map(EventGuestRequest::vehicleId).filter(Objects::nonNull).forEach(vehicleIds::add);
        var vs=batch(vehicleIds,historical==null?vehicles::findDetailsBatch:vehicles::findPresentationDetailsBatch).stream().collect(Collectors.toMap(EventVehicleQuery.Details::id,v->v));
        vs.values().forEach(v->userIds.add(v.ownerId()));
        var us=batch(userIds,users::findByIds).stream().collect(Collectors.toMap(UserDetails::id,u->u));
        var ds=batch(depIds,historical==null?dependents::findDetailsByIds:dependents::findPresentationDetailsByIds).stream().collect(Collectors.toMap(DependentsQuery.Details::id,d->d));
        var all=EventReportPeople.assemble(registrations,guests,us,ds);
        var selected=all.stream().filter(p->status==null || status.equals(p.registrationStatus())).filter(p->type==null || type.equals(p.type()))
            .filter(p->withVehicle==null || withVehicle==(p.vehicleId()!=null))
            .sorted(Comparator.comparing(Person::userId).thenComparing(Person::vehicleId,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(p->p.type().equals("MEMBER")?0:1)).toList();
        var metadata=new ArrayList<String>();metadata.add("ID do evento: "+eventId);
        metadata.add("Evento: "+(event==null?historical.title():event.getTitle()));
        metadata.add("Início: "+ExportValues.text(event==null?historical.startsAt():event.getStartsAt()));
        metadata.add("Término: "+ExportValues.text(event==null?historical.endsAt():event.getEndsAt()));
        metadata.add("Situação: "+(event==null?historical.status():event.effectiveStatus(clock.instant())));
        if(historical!=null)metadata.add("Contexto histórico — excluído em: "+ExportValues.text(historical.deletedAt()));
        if(status!=null)metadata.add("Inscrição: "+status);if(type!=null)metadata.add("Tipo: "+type);if(withVehicle!=null)metadata.add("Com veículo: "+ExportValues.text(withVehicle));
        // The shareable access/coverage files intentionally omit financial and administrative summaries.
        if(product!=Product.ACCESS && product!=Product.HEALTH_COVERAGE) {
            metadata.add("Inscrições: "+registrations.size()+"; confirmados: "+registrations.stream().filter(r->r.status()==EventRegistration.Status.CONFIRMED).count()+"; cancelados: "+registrations.stream().filter(r->r.status()==EventRegistration.Status.CANCELLED).count());
            metadata.add("Dependentes: "+dashboard.dependents()+"; convidados pendentes: "+dashboard.guestsPending()+"; aprovados: "+dashboard.guestsApproved());
            metadata.add("Pessoas: "+dashboard.totalPeople()+"; veículos: "+dashboard.vehicles()+"; capacidade: "+dashboard.totalCapacity()+"; vagas: "+dashboard.availableSeats());
            metadata.add("Cobranças não pagas: "+dashboard.unpaidCharges()+"; aguardando validação: "+dashboard.pendingValidation()+"; pós-cutoff: "+dashboard.postCutoffPending());
        }
        List<String> columns=switch(product) {
            case MANIFEST->List.of("Nome","CPF","Tipo","Titular","ID da inscrição","Situação da inscrição","Veículo","Placa","Situação financeira");
            case ACCESS->List.of("Nome","CPF","Tipo","Titular","Veículo","Placa","Confirmação");
            case TRANSPORT->List.of("ID do veículo","Proprietário","CPF do proprietário","Veículo","Placa","Capacidade","Ocupantes","Dependentes","Convidados","Ocupação total","Lugares livres","Pendência");
            case FINANCIAL->List.of("ID do usuário","Nome","CPF","ID da inscrição","Situação da inscrição","Definição","Valor","Obrigatória para participação","Limite para participação","Vencimento financeiro","ID da cobrança","Situação efetiva da cobrança","Situação do pagamento","Pagamento enviado em","Aguardando validação","Situação para participação");
            case HEALTH_COVERAGE->List.of("Participante","Tipo","Titular","Possui ficha médica");
            case POST_EVENT->List.of("Seção","Identificação","Descrição","Situação","Valor","Veículo","Ocupação","Vagas");
        };
        return renderer.render(new ExportDocument("evento-"+product.name().toLowerCase(Locale.ROOT).replace('_','-'),title(product),columns,metadata,sink->{
            switch(product) {
                case MANIFEST,ACCESS->{for(var p:selected){var v=vs.get(p.vehicleId());String group=p.holder()+" / "+vehicle(v);
                    if(product==Product.ACCESS)sink.accept(ExportRow.of(p.name(),p.cpf(),personType(p.type()),p.holder(),vehicle(v),v==null?"":v.plate(),p.registrationStatus()).grouped(group));
                    else sink.accept(ExportRow.of(p.name(),p.cpf(),personType(p.type()),p.holder(),p.registrationId(),p.registrationStatus(),vehicle(v),v==null?"":v.plate(),financialSummary(p.userId(),dashboard.financial())).grouped(group));}}
                case TRANSPORT->{
                    var selectedVehicles=vs;
                    boolean filtered=status!=null || type!=null || withVehicle!=null;
                    if(filtered)selectedVehicles=vs.entrySet().stream().filter(e->selected.stream().anyMatch(p->Objects.equals(p.vehicleId(),e.getKey()))).collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
                    transport(view,view==TransportView.ALL || view==TransportView.FULL || view==TransportView.AVAILABLE?all:selected,selectedVehicles,us,operations.offers(eventId),sink);
                }
                case FINANCIAL->financial(registrations,rules,dashboard.financial(),us,status,sink);
                case HEALTH_COVERAGE->{
                    var eligible=selected.stream().filter(p->!p.type().equals("GUEST") && !p.registrationStatus().equals("CANCELLED"))
                        .filter(p->!p.type().equals("DEPENDENT") || ds.containsKey(p.personId()) && ds.get(p.personId()).userId().equals(p.userId())).toList();
                    var coveredUsers=covered(MedicalProfileOwner.USER,eligible.stream().filter(p->p.type().equals("MEMBER")).map(Person::personId).toList());
                    var coveredDeps=covered(MedicalProfileOwner.DEPENDENT,eligible.stream().filter(p->p.type().equals("DEPENDENT")).map(Person::personId).toList());
                    for(var p:eligible)sink.accept(ExportRow.of(p.name(),p.type().equals("MEMBER")?"USER":"DEPENDENT",p.holder(),(p.type().equals("MEMBER")?coveredUsers:coveredDeps).contains(p.personId())));}
                case POST_EVENT->{
                    sink.accept(ExportRow.of("Evento",eventId,event==null?historical.title():event.getTitle(),event==null?historical.status():event.effectiveStatus(clock.instant()),"","","","").grouped("Evento"));
                    sink.accept(ExportRow.of("Datas",eventId,"Início: "+ExportValues.text(event==null?historical.startsAt():event.getStartsAt())+"; término: "+ExportValues.text(event==null?historical.endsAt():event.getEndsAt()),historical==null?"Atual":"Histórico — excluído em: "+ExportValues.text(historical.deletedAt()),"","","","").grouped("Evento"));
                    for(var rule:rules)sink.accept(ExportRow.of("Regras",rule.chargeDefinitionId(),"Obrigatória para participação: "+ExportValues.text(rule.requiredForParticipation())+"; limite: "+ExportValues.text(rule.participationCutoff())+"; vencimento: "+ExportValues.text(rule.financialDueDate()),"","","","","").grouped("Regras financeiras e de participação"));
                    for(var p:all)sink.accept(ExportRow.of("Participantes",p.personId(),p.name()+" / "+p.holder(),p.registrationStatus(),"",historicalVehicle(p.vehicleId(),vs.get(p.vehicleId())),"","").grouped("Participantes"));
                    for(var v:vs.values()){long occupied=all.stream().filter(p->Objects.equals(p.vehicleId(),v.id()) && occupies(p)).count();sink.accept(ExportRow.of("Transporte",v.id(),vehicle(v),"","",v.plate(),occupied,Math.max(0,v.seatingCapacity()-occupied)).grouped("Transporte"));}
                    for(var missingId:vehicleIds)if(!vs.containsKey(missingId))sink.accept(ExportRow.of("Transporte",missingId,"Cadastro do veículo indisponível","","","",all.stream().filter(p->Objects.equals(p.vehicleId(),missingId) && occupies(p)).count(),"").grouped("Transporte"));
                    for(var s:dashboard.financial())sink.accept(ExportRow.of("Cobranças",s.memberChargeId(),s.definitionName(),s.effectiveStatus()+" / "+ExportValues.text(s.paymentStatus()),s.amount(),"","","").grouped("Financeiro"));
                    finance.paymentCounts(eventId).forEach((state,count)->sink.accept(ExportRow.of("Pagamentos",state,"Quantidade de pagamentos",state,count,"","","").grouped("Pagamentos")));
                    sink.accept(ExportRow.of("Pendências","","Após limite para participação","",dashboard.postCutoffPending(),"","","").grouped("Pendências"));}
            }
        }),format);
    }
    private void financial(List<EventRegistration> registrations,List<EventChargeRule> rules,List<EventFinancialQuery.State> states,Map<Long,UserDetails> users,String filter,Consumer<ExportRow> sink) {
        var definitions=batch(rules.stream().map(EventChargeRule::chargeDefinitionId).distinct().toList(),finance::definitions).stream().collect(Collectors.toMap(EventFinanceReportQuery.Definition::id,d->d));
        var indexed=states.stream().collect(Collectors.toMap(s->s.userId()+":"+s.chargeDefinitionId(),s->s,(a,b)->a));
        for(var r:registrations)if(filter==null || filter.equals(r.status().name()))for(var rule:rules) {
            var s=indexed.get(r.userId()+":"+rule.chargeDefinitionId());var d=definitions.get(rule.chargeDefinitionId());var u=users.get(r.userId());
            boolean satisfied=EventParticipationRequirement.satisfied(rule,s);
            boolean pending=s!=null && "PENDING_VALIDATION".equals(s.paymentStatus());
            boolean cutoff=!clock.instant().isBefore(rule.participationCutoff());
            String situation=r.status()==EventRegistration.Status.CANCELLED?"Inscrição cancelada"
                :!rule.requiredForParticipation()?"Não obrigatória para participação"
                :satisfied?(pending?"Obrigação satisfeita; aguardando validação":"Obrigação satisfeita")
                :r.status()==EventRegistration.Status.CONFIRMED?"Confirmado com pendência financeira"
                :cutoff?"Passou do limite de participação":pending?"Aguardando validação; obrigação não satisfeita":"Falta pagar";
            sink.accept(ExportRow.of(r.userId(),u==null?"Cadastro indisponível":u.name(),u==null?"":u.cpf(),r.id(),r.status(),s!=null&&s.definitionName()!=null?s.definitionName():d==null?rule.chargeDefinitionId():d.name(),s!=null?s.amount():d==null?null:d.amount(),rule.requiredForParticipation(),rule.participationCutoff(),s!=null?s.dueDate():rule.financialDueDate(),s==null?null:s.memberChargeId(),s==null?"Cobrança ausente":s.effectiveStatus(),s==null?null:s.paymentStatus(),s==null?null:s.paymentSubmittedAt(),pending,situation).grouped("Usuário: "+(u==null?r.userId():u.name())));
        }
    }
    private void transport(TransportView view,List<Person> all,Map<Long,EventVehicleQuery.Details> vs,Map<Long,UserDetails> us,List<EventRideOffer> offers,Consumer<ExportRow> sink) {
        if(view==TransportView.UNALLOCATED_PEOPLE || view==TransportView.UNALLOCATED_GUESTS || view==TransportView.PENDING_GUESTS) {
            for(var p:all)if(!p.registrationStatus().equals("CANCELLED") && (view==TransportView.UNALLOCATED_PEOPLE?p.vehicleId()==null:view==TransportView.UNALLOCATED_GUESTS?p.type().equals("GUEST")&&p.vehicleId()==null:p.type().equals("GUEST")&&"PENDING".equals(p.guestStatus())))
                sink.accept(ExportRow.of(null,p.holder(),"","Não alocado","","",p.name(),p.type().equals("DEPENDENT")?p.name():"",p.type().equals("GUEST")?p.name():"","","",p.registrationStatus()));
            return;
        }
        if(view==TransportView.ACCEPTED_RIDES) {
            for(var o:offers)if(o.status()==EventRideOffer.Status.ACCEPTED){var v=vs.get(o.vehicleId());sink.accept(ExportRow.of(o.vehicleId(),us.containsKey(o.userId())?us.get(o.userId()).name():o.userId(),"",vehicle(v),v==null?"":v.plate(),v==null?"":v.seatingCapacity(),"","",o.guestRequestId(),"","","Carona aceita, ainda não selecionada"));}return;
        }
        for(var v:vs.values().stream().sorted(Comparator.comparing(EventVehicleQuery.Details::id)).toList()) {
            var occupants=all.stream().filter(p->Objects.equals(p.vehicleId(),v.id()) && occupies(p)).toList();
            long free=Math.max(0,v.seatingCapacity()-occupants.size());
            if(view==TransportView.FULL && free>0 || view==TransportView.AVAILABLE && free==0)continue;
            var owner=us.get(v.ownerId());
            sink.accept(ExportRow.of(v.id(),owner==null?"Cadastro indisponível":owner.name(),owner==null?"":owner.cpf(),vehicle(v),v.plate(),v.seatingCapacity(),names(occupants),names(occupants.stream().filter(p->p.type().equals("DEPENDENT")).toList()),names(occupants.stream().filter(p->p.type().equals("GUEST")).toList()),occupants.size(),free,occupants.size()>v.seatingCapacity()?"Capacidade excedida após alteração do veículo":"").grouped("Veículo: "+v.plate()));
        }
    }
    private static boolean occupies(Person p){return !List.of("CANCELLED","REJECTED","PENDING").contains(p.registrationStatus());}
    private static String names(List<Person> people){return people.stream().map(Person::name).collect(Collectors.joining("; "));}
    private static String vehicle(EventVehicleQuery.Details v){return v==null?"Não alocado":ExportValues.text(v.nickname())+" / "+v.brand()+" "+v.model();}
    private static String historicalVehicle(Long id, EventVehicleQuery.Details v) {
        return id == null ? "Não alocado" : v == null ? "Veículo indisponível (ID "+id+")" : vehicle(v);
    }
    private static String personType(String t){return switch(t){case "MEMBER"->"Membro";case "DEPENDENT"->"Dependente";default->"Convidado";};}
    private static String financialSummary(Long user,List<EventFinancialQuery.State> states){return states.stream().filter(s->s.userId().equals(user)).map(s->s.effectiveStatus()+" / "+ExportValues.text(s.paymentStatus())).distinct().collect(Collectors.joining("; "));}
    private static String title(Product p){return switch(p){case MANIFEST->"Manifesto de participantes";case TRANSPORT->"Transporte e ocupação";case FINANCIAL->"Financeiro pré-evento";case ACCESS->"Controle de acesso";case HEALTH_COVERAGE->"Cobertura de ficha médica";case POST_EVENT->"Relatório pós-evento";};}
    private static <T> List<T> batch(Collection<Long> ids,Function<Collection<Long>,List<T>> query){var input=new ArrayList<>(ids);var result=new ArrayList<T>();for(int i=0;i<input.size();i+=500)result.addAll(query.apply(input.subList(i,Math.min(i+500,input.size()))));return result;}
    private Set<Long> covered(MedicalProfileOwner type,List<Long> ids){var result=new HashSet<Long>();for(int i=0;i<ids.size();i+=500)result.addAll(coverage.findCovered(type,ids.subList(i,Math.min(i+500,ids.size()))));return result;}
    @org.springframework.transaction.annotation.Transactional
    public ExportFile emergency(Long eventId,MedicalProfileOwner type,Long target,Long actor) {
        var profile=administration.health(eventId,type,target,actor);
        String name="Cadastro indisponível",cpf="";
        if(type==MedicalProfileOwner.USER){var u=users.findById(target);if(u.isPresent()){name=u.get().name();cpf=u.get().cpf();}}
        else {var d=dependents.findDetailsByIds(List.of(target));if(!d.isEmpty()){name=d.get(0).name();cpf=d.get(0).cpf();}}
        var row=ExportRow.of(name,cpf,type,target,profile.bloodType(),profile.allergies(),profile.chronicConditions(),profile.continuousMedications(),profile.healthInsuranceProvider(),profile.healthInsurancePlan(),profile.healthInsuranceNumber(),profile.emergencyContactName(),profile.emergencyContactPhone(),profile.emergencyContactRelationship(),profile.observations());
        return renderer.render(new ExportDocument("ficha-emergencial","Ficha médica emergencial individual",List.of("Nome","CPF","Tipo","ID do participante","Tipo sanguíneo","Alergias","Condições crônicas","Medicamentos contínuos","Convênio","Plano","Número do convênio","Contato de emergência","Telefone de emergência","Parentesco","Observações"),List.of("Evento: "+eventId),sink->sink.accept(row)),ExportFormat.PDF);
    }
}
