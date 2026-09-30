package com.jeepclub.backend.health.core.application.service.medicalprofile;
import com.jeepclub.backend.health.core.repository.MedicalProfileExportQuery;
import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.shared.export.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.*;
@org.springframework.stereotype.Service @lombok.RequiredArgsConstructor
public class MedicalProfileExportService {
    private final MedicalProfileExportQuery query;
    private final UserQuery users;
    private final DependentsQuery dependents;
    private final ExportRenderer renderer;
    private static final List<String> COLUMNS=List.of("ID do perfil", "Tipo do titular", "ID do titular", "Tipo sanguíneo", "Alergias", "Condições crônicas", "Medicamentos contínuos", "Convênio", "Plano de saúde", "Número do convênio", "Contato de emergência", "Telefone de emergência", "Parentesco do contato", "Observações", "Criado em", "Atualizado em");
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public ExportFile export(ExportFormat format,boolean history,Long profileId,Long userId,Long dependentId,boolean household) {
        for(Long id:Arrays.asList(profileId,userId,dependentId))if(id!=null && id<=0)throw new ExportException(ExportException.Reason.INVALID_FILTER);
        if((profileId!=null?1:0)+(userId!=null?1:0)+(dependentId!=null?1:0)>1 || household && userId==null)
            throw new ExportException(ExportException.Reason.INVALID_FILTER);
        if(userId!=null && !users.existsById(userId) || dependentId!=null && !dependents.existsById(dependentId))
            throw new ExportException(ExportException.Reason.NOT_FOUND);
        var cols=new ArrayList<>(COLUMNS);
        if(history)cols.set(0,"ID do histórico");
        if(history)cols.addAll(List.of("ID do perfil original","Excluído por","Excluído em"));
        if(household)cols.addAll(List.of("Nome do titular","CPF do titular","Nome do dependente","CPF do dependente","Parentesco","Possui ficha médica"));
        String type=userId!=null?"USER":dependentId!=null?"DEPENDENT":null;
        Long owner=userId!=null?userId:dependentId;
        var filters=owner==null?List.<String>of():List.of("Titular: "+owner,"Tipo: "+type);
        return renderer.render(new ExportDocument(history?"historico-perfis-medicos":"perfis-medicos",history?"Histórico de perfis médicos":"Perfis médicos",cols,filters,sink->{
            if(household) { household(userId,sink);return; }
            boolean[] found={false};
            for(int offset=0;;offset+=ExportPages.CHUNK) {
                if(offset>=100000)throw new ExportException(ExportException.Reason.LIMIT);
                var batch=query.read(history,profileId,type,owner==null?null:List.of(owner),offset);
                Set<Long> activeUsers=history?Set.of():users.findAdministrativelyActiveUserIdsByIds(batch.stream().filter(e->e.ownerType().equals("USER")).map(MedicalProfileExportQuery.Entry::ownerId).toList());
                Set<Long> activeDependents=history?Set.of():dependents.findActiveDependentIdsByIds(batch.stream().filter(e->e.ownerType().equals("DEPENDENT")).map(MedicalProfileExportQuery.Entry::ownerId).toList());
                for(var e:batch)if(history || (e.ownerType().equals("USER")?activeUsers:activeDependents).contains(e.ownerId())) {sink.accept(e.row());found[0]=true;}
                if(batch.size()<ExportPages.CHUNK)break;
            }
            if(!found[0] && (profileId!=null || owner!=null))throw new ExportException(ExportException.Reason.NOT_FOUND);
        }),format);
    }
    private void household(Long userId,java.util.function.Consumer<ExportRow> sink) {
        var user=users.findById(userId).orElseThrow(()->new ExportException(ExportException.Reason.NOT_FOUND));
        var own=query.read(false,null,"USER",List.of(userId),0);
        sink.accept(householdRow(user,null,own.isEmpty()?null:own.get(0)));
        long after=0;
        while(true) {
            var batch=dependents.findDetailsByUser(userId,after,ExportPages.CHUNK);
            if(batch.isEmpty())break;
            var profiles=query.read(false,null,"DEPENDENT",batch.stream().map(DependentsQuery.Details::id).toList(),0)
                .stream().collect(Collectors.toMap(MedicalProfileExportQuery.Entry::ownerId,e->e));
            for(var d:batch)sink.accept(householdRow(user,d,profiles.get(d.id())));
            after=batch.get(batch.size()-1).id();
            if(batch.size()<ExportPages.CHUNK)break;
        }
    }
    private ExportRow householdRow(UserDetails user,DependentsQuery.Details d,MedicalProfileExportQuery.Entry profile) {
        var cells=profile==null?new ArrayList<>(Collections.nCopies(COLUMNS.size(),"")):new ArrayList<>(profile.row().cells());
        if(profile==null){cells.set(1,d==null?"USER":"DEPENDENT");cells.set(2,String.valueOf(d==null?user.id():d.id()));}
        cells.add(user.name());cells.add(ExportValues.text(user.cpf()));cells.add(d==null?"":d.name());cells.add(d==null?"":ExportValues.text(d.cpf()));cells.add(d==null?"":d.relationshipType());cells.add(profile==null?"Não":"Sim");
        return new ExportRow(null,cells).grouped("Titular: "+user.name()+" | CPF: "+user.cpf(),16,17);
    }
}
