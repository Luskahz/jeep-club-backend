package com.jeepclub.backend.health.infra.persistence.query;
import com.jeepclub.backend.health.core.repository.MedicalProfileExportQuery;
import com.jeepclub.backend.shared.export.*;
import java.util.*;
@org.springframework.stereotype.Repository @lombok.RequiredArgsConstructor
public class MedicalProfileExportJpaQuery implements MedicalProfileExportQuery {
    private final jakarta.persistence.EntityManager em;
    public List<Entry> read(boolean history,Long id,String type,Collection<Long> ids,int offset) {
        String selection="m.id,m.ownerType,m.ownerId,m.bloodType,m.allergies,m.chronicConditions,m.continuousMedications,m.healthInsuranceProvider,m.healthInsurancePlan,m.healthInsuranceNumber,m.emergencyContactName,m.emergencyContactPhone,m.emergencyContactRelationship,m.observations,m.createdAt,m.updatedAt"+(history?",m.medicalProfileId,m.deletedByUserId,m.deletedAt":"");
        String entity=history?"MedicalProfileHistoryEntity":"MedicalProfileEntity";
        var q=em.createQuery("select "+selection+" from "+entity+" m where (:id is null or m.id=:id)"+
            (type==null?"":" and cast(m.ownerType as string)=:type")+(ids==null?"":" and m.ownerId in :ids")+" order by m.id",Object[].class).setParameter("id",id);
        if(type!=null)q.setParameter("type",type);
        if(ids!=null){if(ids.isEmpty())return List.of();q.setParameter("ids",ids);}
        return q.setFirstResult(offset).setMaxResults(ExportPages.CHUNK).getResultList().stream()
            .map(t->new Entry(t[1].toString(),(Long)t[2],ExportRow.of(t))).toList();
    }
}
