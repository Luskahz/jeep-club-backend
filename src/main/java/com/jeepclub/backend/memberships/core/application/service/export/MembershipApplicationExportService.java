package com.jeepclub.backend.memberships.core.application.service.export;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationExportQuery;
import com.jeepclub.backend.shared.export.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class MembershipApplicationExportService {
    private final MembershipApplicationExportQuery query;
    private final ExportRenderer renderer;
    
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public ExportFile export(ExportFormat format, Long id, Long ownerId, String name, String status, java.time.Instant from, java.time.Instant to) {
        if (id != null && id <= 0 || ownerId != null && ownerId <= 0 || from != null && to != null && from.isAfter(to))
            throw new ExportException(ExportException.Reason.INVALID_FILTER);
        if (status != null && !List.of("PENDING", "APPROVED", "REJECTED", "COMPLETED").contains(status)) throw new ExportException(ExportException.Reason.INVALID_FILTER);
        
        var filters=new ArrayList<String>();
        if(id!=null)filters.add("ID: "+id);
        if(ownerId!=null)filters.add("Titular: "+ownerId);
        if(name!=null)filters.add("Nome: "+name);
        if(status!=null)filters.add("Situação: "+status);
        if(from!=null)filters.add("De: "+ExportValues.text(from));
        if(to!=null)filters.add("Até: "+ExportValues.text(to));
        return renderer.render(new ExportDocument("solicitacoes-adesao", "Solicitações de adesão", List.of("ID", "Nome", "CPF", "E-mail", "Telefone", "Mensagem", "Situação", "Motivo da rejeição", "Revisado por", "ID do usuário criado", "Solicitado em", "Revisado em", "Concluído em", "Atualizado em"), filters, sink -> {
            for(int offset=0;;offset+=ExportPages.CHUNK) {
                var batch=query.read(id,ownerId,name,status,from,to,offset);
                if(offset==0 && id!=null && batch.isEmpty()) throw new ExportException(ExportException.Reason.NOT_FOUND);
                batch.forEach(entry -> sink.accept(entry.row()));

                if(batch.size()<ExportPages.CHUNK)break;
            }
        }), format);
    }
}
