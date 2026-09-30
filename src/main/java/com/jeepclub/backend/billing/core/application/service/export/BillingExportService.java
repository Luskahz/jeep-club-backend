package com.jeepclub.backend.billing.core.application.service.export;
import com.jeepclub.backend.billing.core.repository.BillingExportQuery;
import com.jeepclub.backend.billing.core.repository.BillingExportQuery.Product;
import com.jeepclub.backend.billing.core.application.query.BillingExportFilter;
import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.iam.authorization.api.module.RolePresentationQuery;
import com.jeepclub.backend.publications.api.module.EventPresentationQuery;
import com.jeepclub.backend.shared.export.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.*;
@org.springframework.stereotype.Service @lombok.RequiredArgsConstructor
public class BillingExportService {
    private final BillingExportQuery query;
    private final UserQuery users;
    private final RolePresentationQuery roles;
    private final EventPresentationQuery events;
    private final ExportRenderer renderer;
    private final Clock clock;
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public ExportFile export(Product product,BillingExportFilter f,ExportFormat format) {
        f.validate();
        boolean financial=product==Product.CHARGES || product==Product.PAYMENTS || product==Product.REFUNDS;
        if(!financial && (f.chargeStatus()!=null || f.effectiveStatus()!=null)
            || product!=Product.PAYMENTS && f.paymentMethod()!=null
            || product==Product.CYCLES && f.userId()!=null
            || product==Product.DEFINITIONS && (f.chargeCycleId()!=null || f.year()!=null || f.month()!=null || f.dueFrom()!=null || f.dueTo()!=null))
            throw new ExportException(ExportException.Reason.INVALID_FILTER);
        BillingExportFilter.check(f.status(),switch(product){case DEFINITIONS->"ACTIVE INACTIVE ARCHIVED";case CYCLES->"GENERATED CANCELED FINISHED ARCHIVED";case CHARGES->"PENDING PAID CANCELED";case PAYMENTS->"PENDING_VALIDATION CONFIRMED REJECTED CANCELED";case REFUNDS->"ELIGIBLE REQUESTED APPROVED REJECTED REFUNDED EXPIRED CANCELED";});
        if(f.paymentMethod()!=null) {
            try {com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod.valueOf(f.paymentMethod());}
            catch(IllegalArgumentException e){throw new ExportException(ExportException.Reason.INVALID_FILTER);}
        }
        if(f.userId()!=null && !users.existsById(f.userId()) || f.eventId()!=null && !query.hasEventContext(f.eventId())
            || f.chargeDefinitionId()!=null && !query.exists(Product.DEFINITIONS,f.chargeDefinitionId())
            || f.chargeCycleId()!=null && !query.exists(Product.CYCLES,f.chargeCycleId())
            || f.id()!=null && !query.exists(product,f.id())) throw new ExportException(ExportException.Reason.NOT_FOUND);
        var columns=switch(product) {
            case DEFINITIONS -> List.of("ID","Nome","Descrição","Valor padrão","Recorrência","Obrigatória","Política de pagamento","Dias de tolerância","Situação","Criado em","Atualizado em","Arquivado em","ID da atribuição","Tipo de público","Atribuição ativa","Atribuição criada em","Atribuição atualizada em","ID do alvo","Nome do usuário","CPF do usuário","Nome do papel","Título do evento","Início do evento");
            case CYCLES -> List.of("ID","ID da definição","Nome da definição na geração","Descrição na geração","Valor na geração","Recorrência na geração","Obrigatória na geração","Política na geração","Dias de tolerância na geração","Código do ciclo","Vencimento","Situação","Gerado por","Gerado em","Cancelado em","Cancelado por","Concluído em","Concluído por","Arquivado em","Arquivado por","Criado em","Atualizado em","Nome do usuário","CPF do usuário","Nome do papel","Título do evento","Início do evento");
            case CHARGES -> List.of("ID","ID do usuário","ID da definição","ID do ciclo","Valor original","Valor final","Vencimento","Política de pagamento","Dias de tolerância","Pagamento permitido até","Situação","Criado em","Atualizado em","Pago em","Cancelado em","Definição na geração","Código do ciclo","Recorrência","Ano de referência","Mês de referência","ID do evento","Nome do usuário","CPF do usuário","Nome do papel","Título do evento","Início do evento","Situação efetiva da cobrança","ID do último pagamento","Valor do último pagamento","Método do último pagamento","Situação do último pagamento","Último pagamento enviado em");
            case PAYMENTS -> List.of("ID","ID da cobrança","Valor","Método de pagamento","Situação","Pago em","Confirmado em","Confirmado por","Rejeitado em","Rejeitado por","Motivo da rejeição","Cancelado em","Observações","Criado em","Atualizado em","Enviado em","ID do usuário","ID da definição","ID do ciclo","Definição na geração","Código do ciclo","Recorrência","Ano de referência","Mês de referência","ID do evento","Vencimento da cobrança","Nome do usuário","CPF do usuário","Nome do papel","Título do evento","Início do evento","Situação efetiva da cobrança");
            case REFUNDS -> List.of("ID","ID da cobrança","ID do pagamento","ID do ciclo","ID do usuário","Valor","Motivo","Situação","Elegível em","Elegível até","Criado por","Solicitado em","Solicitado por","Aprovado em","Aprovado por","Rejeitado em","Rejeitado por","Motivo da rejeição","Reembolsado em","Reembolsado por","Cancelado em","Cancelado por","Criado em","Atualizado em","ID da definição","Definição na geração","Código do ciclo","Recorrência","Ano de referência","Mês de referência","ID do evento","Valor originalmente pago","Nome do usuário","CPF do usuário","Nome do papel","Título do evento","Início do evento","Situação efetiva da cobrança");
        };
        String name=switch(product){case DEFINITIONS->"definicoes-atribuicoes";case CYCLES->"ciclos";case CHARGES->"cobrancas";case PAYMENTS->"pagamentos";case REFUNDS->"reembolsos";};
        LocalDate today=LocalDate.now(clock);
        return renderer.render(new ExportDocument(name,"Relatório financeiro: "+name,columns,f.labels(),sink->{
            for(int offset=0;;offset+=ExportPages.CHUNK) {
                if(offset>=100000)throw new ExportException(ExportException.Reason.LIMIT);
                var batch=query.read(product,f,offset);
                var latest=product==Product.CHARGES?query.latestPayments(batch.stream().map(e->e.charge().getId()).toList()):Map.<Long,ExportRow>of();
                var us=users.findByIds(batch.stream().map(BillingExportQuery.Entry::userId).filter(Objects::nonNull).distinct().toList()).stream().collect(Collectors.toMap(UserDetails::id,u->u));
                var rs=roles.findByIds(batch.stream().map(BillingExportQuery.Entry::roleId).filter(Objects::nonNull).distinct().toList()).stream().collect(Collectors.toMap(RolePresentationQuery.Details::id,r->r));
                var es=events.findByIds(batch.stream().map(BillingExportQuery.Entry::eventId).filter(Objects::nonNull).distinct().toList()).stream().collect(Collectors.toMap(EventPresentationQuery.Details::id,e->e));
                for(var e:batch) {
                    String effective=e.charge()==null?null:e.charge().effectiveStatusAt(today).name();
                    if(f.effectiveStatus()!=null && !f.effectiveStatus().equals(effective))continue;
                    var u=us.get(e.userId());var r=rs.get(e.roleId());var event=es.get(e.eventId());
                    var cells=new ArrayList<>(e.row().cells());
                    cells.add(u==null?"":u.name());cells.add(u==null?"":ExportValues.text(u.cpf()));cells.add(r==null?"":r.name());cells.add(event==null?"":event.title());cells.add(event==null?"":ExportValues.text(event.startsAt()));
                    if(e.charge()!=null)cells.add(switch(effective){case "PENDING"->"Pendente";case "OVERDUE"->"Vencida";case "EXPIRED"->"Expirada";case "PAID"->"Paga";default->"Cancelada";});
                    if(product==Product.CHARGES)cells.addAll(latest.getOrDefault(e.charge().getId(),ExportRow.of("","","","","")).cells());
                    sink.accept(new ExportRow(f.chargeCycleId()==null?null:"Ciclo: "+f.chargeCycleId()+(product==Product.CHARGES?" — "+e.row().cells().get(16)+" — "+e.row().cells().get(15):""),cells));
                }
                if(batch.size()<ExportPages.CHUNK)break;
            }
        }),format);
    }
}
