package com.jeepclub.backend.iam.authorization.infra.persistence.query;
import com.jeepclub.backend.iam.authorization.core.repository.UserAccessExportQuery;
import com.jeepclub.backend.shared.export.ExportException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.time.Instant;
@Repository @RequiredArgsConstructor
public class UserAccessExportJpaQuery implements UserAccessExportQuery {
    private final EntityManager em;
    public List<RoleAssignment> roles(Collection<Long> ids) {
        var rows=em.createQuery("select u.userId,r.id,r.name,r.description,r.kind,r.status,u.createdAt from UserRoleEntity u join u.role r where u.userId in :ids order by u.userId,r.id",Object[].class)
            .setParameter("ids",ids).setMaxResults(20001).getResultList();
        check(rows.size());
        return rows.stream().map(t->new RoleAssignment((Long)t[0],(Long)t[1],(String)t[2],(String)t[3],t[4].toString(),t[5].toString(),(Instant)t[6])).toList();
    }
    public List<EffectivePermission> permissions(Collection<Long> ids) {
        var rows=em.createQuery("select distinct u.userId,p.module,p.code from UserRoleEntity u join u.role r, RolePermissionEntity rp join rp.permission p where rp.role.id=r.id and cast(r.status as string)='ACTIVE' and u.userId in :ids order by u.userId,p.code",Object[].class)
            .setParameter("ids",ids).setMaxResults(20001).getResultList();
        check(rows.size());
        return rows.stream().map(t->new EffectivePermission((Long)t[0],t[1].toString(),t[2].toString())).toList();
    }
    private void check(int count) { if(count>20000) throw new ExportException(ExportException.Reason.LIMIT); }
}
