package com.jeepclub.backend.vehicles.core.application.service.export;
import com.jeepclub.backend.vehicles.core.repository.VehicleExportQuery;
import com.jeepclub.backend.shared.export.*;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class VehicleExportService {
    private final VehicleExportQuery query;
    private final ExportRenderer renderer;
    private final UserQuery users;
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public ExportFile export(ExportFormat format, Long id, Long ownerId, String name, String status, java.time.Instant from, java.time.Instant to) {
        if (id != null && id <= 0 || ownerId != null && ownerId <= 0 || from != null && to != null && from.isAfter(to))
            throw new ExportException(ExportException.Reason.INVALID_FILTER);
        
        if (ownerId != null && !users.existsById(ownerId)) throw new ExportException(ExportException.Reason.NOT_FOUND);
        var filters=new ArrayList<String>();
        if(id!=null)filters.add("ID: "+id);
        if(ownerId!=null)filters.add("Titular: "+ownerId);
        if(name!=null)filters.add("Nome: "+name);
        if(status!=null)filters.add("Situação: "+status);
        if(from!=null)filters.add("De: "+ExportValues.text(from));
        if(to!=null)filters.add("Até: "+ExportValues.text(to));
        return renderer.render(new ExportDocument("veiculos", "Veículos", List.of("ID", "Apelido", "Possui foto", "Placa", "RENAVAM", "Marca", "Modelo", "Ano de fabricação", "Ano do modelo", "Cor", "Capacidade de passageiros", "Combustível", "Cilindrada", "Situação", "Possui reboque", "ID do proprietário", "Criado em", "Atualizado em", "Nome do titular", "CPF do titular"), filters, sink -> {
            for(int offset=0;;offset+=ExportPages.CHUNK) {
                var batch=query.read(id,ownerId,name,status,from,to,offset);
                if(offset==0 && id!=null && batch.isEmpty()) throw new ExportException(ExportException.Reason.NOT_FOUND);

                var identities=users.findByIds(batch.stream().map(VehicleExportQuery.Entry::ownerId).filter(Objects::nonNull).distinct().toList())
                    .stream().collect(Collectors.toMap(u -> u.id(), u -> u));
                for(var entry:batch) {
                    var u=identities.get(entry.ownerId()); var cells=new ArrayList<>(entry.row().cells());
                    cells.add(u==null?"Cadastro indisponível":u.name());cells.add(u==null?"":ExportValues.text(u.cpf()));
                    var row=new ExportRow(null,cells);
                    sink.accept(ownerId==null?row:row.grouped("Titular: "+(u==null?ownerId:u.name())+" | ID: "+ownerId+" | CPF: "+(u==null?"":u.cpf()),18,19,15));
                }

                if(batch.size()<ExportPages.CHUNK)break;
            }
        }), format);
    }
}
