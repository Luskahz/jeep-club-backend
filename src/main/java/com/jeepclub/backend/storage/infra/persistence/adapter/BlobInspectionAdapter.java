package com.jeepclub.backend.storage.infra.persistence.adapter;

import com.jeepclub.backend.storage.core.repository.*;
import com.jeepclub.backend.storage.infra.persistence.entity.StoredBlobEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.ArrayList;

@Repository @Transactional(readOnly=true)
public class BlobInspectionAdapter implements BlobInspection {
    private final EntityManager em;private final PhysicalBlobStore physical;
    public BlobInspectionAdapter(EntityManager em,PhysicalBlobStore physical) {this.em=em;this.physical=physical;}
    @Override public Report inspect(String after,int limit) {
        if(after==null || !(after.isEmpty() || after.matches("[a-f0-9]{64}")) || limit<1 || limit>100) throw new IllegalArgumentException("Invalid inventory page");
        var rows=em.createQuery("select b from StoredBlobEntity b where b.sha256>:after order by b.sha256",StoredBlobEntity.class)
            .setParameter("after",after).setMaxResults(limit).getResultList();
        var items=new ArrayList<Item>();
        for(var b:rows) {
            long references=em.createQuery("select count(v) from VariantBlobEntity v where v.sha256=:hash",Long.class).setParameter("hash",b.getSha256()).getSingleResult();
            String state=b.isReady()?"READY":"RESERVED";
            try {physical.readVerified(b.getSha256(),b.getSizeBytes());if(!b.isReady()) state="PRESENT_UNCONFIRMED";}
            catch(IllegalStateException unavailable) {state=b.isReady()?"MISSING_OR_CORRUPT":"PENDING_WRITE";}
            if(state.equals("READY") && references==0) state="UNREFERENCED_RETAINED";
            items.add(new Item(b.getSha256(),state,references,references>1));
        }
        // Files outside this page are not considered unknown. Check exact reservations, without inferring ownership.
        var unknown=new ArrayList<String>();
        for(String filename:physical.inventory()) {
            if(filename.matches("[a-f0-9]{64}\\.bin")) {
                if(em.find(StoredBlobEntity.class,filename.substring(0,64))==null) unknown.add(filename);
            } else unknown.add(filename);
        }
        return new Report(List.copyOf(items),List.copyOf(unknown));
    }
}
