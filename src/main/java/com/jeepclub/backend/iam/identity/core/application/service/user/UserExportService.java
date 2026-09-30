package com.jeepclub.backend.iam.identity.core.application.service.user;
import com.jeepclub.backend.iam.identity.core.application.query.user.*;
import com.jeepclub.backend.iam.identity.core.repository.AdminUserQueryRepository;
import com.jeepclub.backend.shared.export.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Service @RequiredArgsConstructor
public class UserExportService {
    private final AdminUserQueryRepository query;
    private final ExportRenderer renderer;
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public ExportFile export(AdminUserFilter filter, ExportFormat format) {
        if(filter.id()!=null && query.findById(filter.id()).isEmpty()) throw new ExportException(ExportException.Reason.NOT_FOUND);
        var filters=new ArrayList<String>();
        if(filter.id()!=null) filters.add("ID: "+filter.id());
        if(filter.name()!=null) filters.add("Nome: "+filter.name());
        if(filter.birthDate()!=null) filters.add("Nascimento: "+ExportValues.text(filter.birthDate()));
        if(filter.email()!=null) filters.add("E-mail: "+filter.email());
        if(filter.cpf()!=null) filters.add("CPF: "+filter.cpf());
        if(filter.rg()!=null) filters.add("RG: "+filter.rg());
        if(filter.phoneNumber()!=null) filters.add("Telefone: "+filter.phoneNumber());
        if(filter.status()!=null) filters.add("Situação: "+filter.status());
        if(filter.createdFrom()!=null) filters.add("Criação desde: "+ExportValues.text(filter.createdFrom()));
        if(filter.createdTo()!=null) filters.add("Criação até: "+ExportValues.text(filter.createdTo()));
        if(filter.updatedFrom()!=null) filters.add("Atualização desde: "+ExportValues.text(filter.updatedFrom()));
        if(filter.updatedTo()!=null) filters.add("Atualização até: "+ExportValues.text(filter.updatedTo()));
        if(filter.query()!=null) filters.add("Busca: "+filter.query());
        return renderer.render(new ExportDocument("usuarios","Usuários",List.of("ID","Nome","Data de nascimento","E-mail","CPF","RG","Telefone","Possui foto","Situação","Criado em","Desativado em","Atualizado em"),filters,
            sink -> ExportPages.read(offset->query.findAll(filter,EnumSet.allOf(AdminUserField.class),PageRequest.of(offset/ExportPages.CHUNK,ExportPages.CHUNK,Sort.by("id"))).getContent(),
                u->sink.accept(ExportRow.of(u.id(),u.name(),u.birthDate(),u.email(),u.cpf(),u.rg(),u.phoneNumber(),u.profilePhotoStorageKey()!=null,u.status(),u.createdAt(),u.disabledAt(),u.updatedAt())))),format);
    }
}
