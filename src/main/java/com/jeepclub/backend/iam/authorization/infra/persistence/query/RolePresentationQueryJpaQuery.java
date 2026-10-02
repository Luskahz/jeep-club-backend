package com.jeepclub.backend.iam.authorization.infra.persistence.query;
import com.jeepclub.backend.iam.authorization.api.module.RolePresentationQuery;
import com.jeepclub.backend.iam.authorization.core.repository.RolePresentationQueryRepository;
@org.springframework.stereotype.Repository @lombok.RequiredArgsConstructor
public class RolePresentationQueryJpaQuery implements RolePresentationQueryRepository {
    private final jakarta.persistence.EntityManager em;
    public java.util.List<RolePresentationQuery.Details> findByIds(java.util.Collection<Long> ids) {
        return em.createQuery("select e.id,e.name from RoleEntity e where e.id in :ids",Object[].class).setParameter("ids",ids)
            .getResultList().stream().map(t->new RolePresentationQuery.Details((Long)t[0],(String)t[1])).toList();
    }
}
