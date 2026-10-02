package com.jeepclub.backend.publications.core.application.service;
import com.jeepclub.backend.publications.core.repository.PublicationExportQuery;
import com.jeepclub.backend.publications.core.repository.PublicationExportQuery.Product;
import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.shared.export.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
@org.springframework.stereotype.Service @lombok.RequiredArgsConstructor
public class PublicationExportService {
    private final PublicationExportQuery query;
    private final UserQuery users;
    private final ExportRenderer renderer;
    private final Clock clock;
    @org.springframework.transaction.annotation.Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public ExportFile export(Product product,Long id,String status,String lifecycle,Instant from,Instant to,ExportFormat format) {
        if(id!=null && id<=0 || from!=null && to!=null && from.isAfter(to))throw new ExportException(ExportException.Reason.INVALID_FILTER);
        var valid=product.name().contains("REQUESTS")?List.of("PENDING","APPROVED","REJECTED"):List.of("DRAFT","PUBLISHED","ARCHIVED");
        if(status!=null && !valid.contains(status) || lifecycle!=null && !List.of("OPEN","IN_PROGRESS","FINISHED","CANCELLED").contains(lifecycle))throw new ExportException(ExportException.Reason.INVALID_FILTER);
        var columns=switch(product) {
case NOTICES -> List.of("ID","ID do autor","Título","Conteúdo","Situação editorial","Criado em","Atualizado em","Publicado em","Arquivado em","Nome do autor");
case NOTICE_HISTORY -> List.of("ID","ID do autor","Título","Conteúdo","Situação editorial","Criado em","Atualizado em","Publicado em","Arquivado em","ID da publicação original","Excluído por","Excluído em","Nome do autor");
case SERVICES -> List.of("ID","ID do autor","Título","Conteúdo","Situação editorial","Criado em","Atualizado em","Publicado em","Arquivado em","ID da solicitação de origem","Valor","Telefone de contato","Nome do autor");
case SERVICE_HISTORY -> List.of("ID","ID do autor","Título","Conteúdo","Situação editorial","Criado em","Atualizado em","Publicado em","Arquivado em","ID da publicação original","Excluído por","Excluído em","ID da solicitação de origem","Valor","Telefone de contato","Nome do autor");
case EVENTS -> List.of("ID","ID do autor","Título","Conteúdo","Situação editorial","Criado em","Atualizado em","Publicado em","Arquivado em","Início","Término","Situação operacional","Situação efetiva do evento","Nome do autor","Regras financeiras e de participação");
case EVENT_HISTORY -> List.of("ID","ID do autor","Título","Conteúdo","Situação editorial","Criado em","Atualizado em","Publicado em","Arquivado em","ID da publicação original","Excluído por","Excluído em","Início","Término","Situação operacional","Situação efetiva do evento","Nome do autor","Regras financeiras e de participação");
case SERVICE_REQUESTS -> List.of("ID","Solicitado por","Título","Conteúdo","Valor","Telefone de contato","Situação editorial","Motivo da rejeição","Revisado por","ID da publicação criada","Solicitado em","Revisado em","Atualizado em","Nome do autor");
case CHANGE_REQUESTS -> List.of("ID","ID da publicação","Solicitado por","Título proposto","Conteúdo proposto","Valor proposto","Telefone proposto","Situação editorial","Motivo da rejeição","Revisado por","Solicitado em","Revisado em","Atualizado em","Nome do autor");
        };
        var filters=new ArrayList<String>();if(id!=null)filters.add("ID: "+id);if(status!=null)filters.add("Situação: "+status);if(lifecycle!=null)filters.add("Situação do evento: "+lifecycle);if(from!=null)filters.add("De: "+ExportValues.text(from));if(to!=null)filters.add("Até: "+ExportValues.text(to));
        var now=clock.instant();
        String title=switch(product) {case NOTICES->"Avisos";case NOTICE_HISTORY->"Histórico de avisos";case SERVICES->"Serviços";case SERVICE_HISTORY->"Histórico de serviços";case EVENTS->"Eventos";case EVENT_HISTORY->"Histórico de eventos";case SERVICE_REQUESTS->"Solicitações de serviço";case CHANGE_REQUESTS->"Solicitações de alteração de serviço";};
        return renderer.render(new ExportDocument("publicacoes-"+product.name().toLowerCase(Locale.ROOT).replace('_','-'),title,columns,filters,sink->{
            for(int offset=0;;offset+=ExportPages.CHUNK) {
                var batch=query.read(product,id,status,lifecycle,from,to,now,offset);
                if(offset==0 && id!=null && batch.isEmpty())throw new ExportException(ExportException.Reason.NOT_FOUND);
                var names=users.findByIds(batch.stream().map(PublicationExportQuery.Entry::authorId).distinct().toList()).stream().collect(Collectors.toMap(UserDetails::id,UserDetails::name));
                var rules=(product==Product.EVENTS || product==Product.EVENT_HISTORY)?query.ruleSummaries(batch.stream().map(e->Long.valueOf(e.row().cells().get(product==Product.EVENT_HISTORY?9:0))).toList()):Map.<Long,String>of();
                for(var e:batch){var cells=new ArrayList<>(e.row().cells());cells.add(names.getOrDefault(e.authorId(),"Cadastro indisponível"));if(product==Product.EVENTS || product==Product.EVENT_HISTORY)cells.add(rules.getOrDefault(Long.valueOf(e.row().cells().get(product==Product.EVENT_HISTORY?9:0)),"Sem regra financeira"));sink.accept(new ExportRow(null,cells));}
                if(batch.size()<ExportPages.CHUNK)break;
            }
        }),format);
    }
}
