package com.jeepclub.backend.vehicles.infra.persistence.query;

import com.jeepclub.backend.vehicles.core.application.exceptions.InvalidVehicleSearchException;
import com.jeepclub.backend.vehicles.core.application.query.*;
import com.jeepclub.backend.vehicles.core.application.result.AdminVehicleSearchResult;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import java.util.*;

/** Module-local tuple query; no Identity persistence access and no entity/gallery materialization. */
public final class AdminVehicleJpaQuery {
    private final EntityManager em;
    private final com.jeepclub.backend.iam.identity.api.module.UserNameCriteria ownerNames;
    public AdminVehicleJpaQuery(EntityManager em, com.jeepclub.backend.iam.identity.api.module.UserNameCriteria ownerNames) {
        this.em=em;
        this.ownerNames=ownerNames;
    }
    public Page<AdminVehicleSearchResult> findAll(AdminVehicleFilter filter, Set<AdminVehicleField> fields, Pageable pageable) {
        Sort sort=pageable.getSort();
        var allowed=Set.of("id","nickname","plate","photo","modelYear","model","color","ownerId","status",
                "brand","manufacturingYear","seatingCapacity","fuelType","engineDisplacement","towing","createdAt","updatedAt","archivedAt");
        for(var order:sort) if(!allowed.contains(order.getProperty())) throw new InvalidVehicleSearchException("Invalid vehicle sort field: "+order.getProperty());
        if(sort.getOrderFor("id")==null) sort=sort.and(Sort.by("id"));
        var stable=PageRequest.of(pageable.getPageNumber(),pageable.getPageSize(),sort);
        var cb=em.getCriteriaBuilder();var query=cb.createTupleQuery();var root=query.from(VehicleEntity.class);
        var persisted=EnumSet.copyOf(fields);
        if(persisted.remove(AdminVehicleField.OWNER_NAME)) persisted.add(AdminVehicleField.OWNER_ID);
        List<Selection<?>> columns=new ArrayList<>();
        for(var field:persisted) columns.add(root.get(field.property()).alias(field.property()));
        query.multiselect(columns).where(predicates(filter,root,cb,query).toArray(Predicate[]::new));
        query.orderBy(sort.stream().map(o -> o.isAscending()?cb.asc(root.get(o.getProperty())):cb.desc(root.get(o.getProperty()))).toList());
        var rows=em.createQuery(query).setFirstResult(Math.toIntExact(stable.getOffset())).setMaxResults(stable.getPageSize()).getResultList();
        var count=cb.createQuery(Long.class);var counted=count.from(VehicleEntity.class);
        count.select(cb.count(counted)).where(predicates(filter,counted,cb,count).toArray(Predicate[]::new));
        return new PageImpl<>(rows.stream().map(t -> result(t,persisted)).toList(),stable,em.createQuery(count).getSingleResult());
    }
    private List<Predicate> predicates(AdminVehicleFilter filter, Root<VehicleEntity> root, CriteriaBuilder cb, CommonAbstractCriteria query) {
        var result=new ArrayList<Predicate>();
        result.add(cb.isFalse(root.get("legacyDeleted")));
        result.add(cb.equal(root.get("status"),filter.status()));
        if(filter.ownerId()!=null) result.add(cb.equal(root.get("ownerId"),filter.ownerId()));
        if(filter.plate()!=null) result.add(cb.like(plate(root,cb),literal(filter.plate()),'!'));
        if(filter.nickname()!=null) result.add(cb.like(cb.lower(root.get("nickname")),literal(filter.nickname()),'!'));
        if(filter.model()!=null) result.add(cb.like(cb.lower(root.get("model")),literal(filter.model()),'!'));
        if(filter.q()!=null) {
            String pattern=literal(filter.q());String normalized=AdminVehicleFilter.normalizePlate(filter.q());
            result.add(cb.or(cb.like(cb.lower(root.get("nickname")),pattern,'!'),cb.like(cb.lower(root.get("model")),pattern,'!'),
                    normalized==null||normalized.isEmpty()?cb.disjunction():cb.like(plate(root,cb),literal(normalized),'!'),
                    ownerNames.matches(cb,query,root.get("ownerId"),filter.q())));
        }
        return result;
    }
    private Expression<String> plate(Root<VehicleEntity> root, CriteriaBuilder cb) {
        Expression<String> value=cb.lower(root.get("plate"));
        for(String mask:List.of("-","."," ")) value=cb.function("replace",String.class,value,cb.literal(mask),cb.literal(""));
        return value;
    }
    private static String literal(String value) {return "%"+value.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
    private static <T> T get(Tuple tuple,Set<AdminVehicleField> fields,AdminVehicleField field,Class<T> type) {
        return fields.contains(field)?tuple.get(field.property(),type):null;
    }
    private AdminVehicleSearchResult result(Tuple t,Set<AdminVehicleField> f) {
        return new AdminVehicleSearchResult(get(t,f,AdminVehicleField.ID,Long.class),get(t,f,AdminVehicleField.NICKNAME,String.class),
                get(t,f,AdminVehicleField.PLATE,String.class),get(t,f,AdminVehicleField.PHOTO,String.class),get(t,f,AdminVehicleField.MODEL_YEAR,Integer.class),
                get(t,f,AdminVehicleField.MODEL,String.class),get(t,f,AdminVehicleField.COLOR,String.class),get(t,f,AdminVehicleField.OWNER_ID,Long.class),
                null,get(t,f,AdminVehicleField.STATUS,VehicleStatus.class));
    }
}
