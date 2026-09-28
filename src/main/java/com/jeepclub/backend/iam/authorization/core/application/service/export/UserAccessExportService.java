package com.jeepclub.backend.iam.authorization.core.application.service.export;
import com.jeepclub.backend.iam.authorization.core.repository.UserAccessExportQuery;
import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.shared.export.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.util.stream.Collectors;
@Service @RequiredArgsConstructor
public class UserAccessExportService {
    private final UserAccessExportQuery query;
    private final UserQuery users;
    private final ExportRenderer renderer;
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public ExportFile export(Long userId,ExportFormat format) {
        if(userId!=null && userId<=0) throw new ExportException(ExportException.Reason.INVALID_FILTER);
        return renderer.render(new ExportDocument("acessos-usuarios","Usuários, papéis e permissões efetivas",
            List.of("ID do usuário","Nome","CPF","Papéis atribuídos","Permissões efetivas"),
            userId==null?List.of():List.of("Usuário: "+userId),sink->{
            long after=0;
            do {
                var batch=userId==null?users.findAfterId(after,100):List.of(users.findById(userId).orElseThrow(()->new ExportException(ExportException.Reason.NOT_FOUND)));
                if(batch.isEmpty()) break;
                var ids=batch.stream().map(UserDetails::id).toList();
                var roles=query.roles(ids).stream().collect(Collectors.groupingBy(UserAccessExportQuery.RoleAssignment::userId));
                var permissions=query.permissions(ids).stream().collect(Collectors.groupingBy(UserAccessExportQuery.EffectivePermission::userId));
                for(var u:batch) {
                    String rs=roles.getOrDefault(u.id(),List.of()).stream().map(r->r.roleName()+" ["+r.roleId()+", "+r.kind()+", "+r.status()+"] — "+ExportValues.text(r.description())+"; atribuído em "+ExportValues.text(r.assignedAt())).collect(Collectors.joining("\n"));
                    String ps=permissions.getOrDefault(u.id(),List.of()).stream().map(p->p.module()+": "+p.code()).collect(Collectors.joining("\n"));
                    sink.accept(ExportRow.of(u.id(),u.name(),u.cpf(),rs,ps).grouped("Usuário: "+u.name()));
                }
                after=batch.get(batch.size()-1).id();
                if(userId!=null || batch.size()<100) break;
            }while(true);
        }),format);
    }
}
