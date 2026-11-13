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
    @Override public Report inspect(String after,int limit,String inventoryCursor,boolean includeFilesystem) {
        if(after==null || !(after.isEmpty() || after.matches("[a-f0-9]{64}")) || limit<1 || limit>100) throw new IllegalArgumentException("Invalid inventory page");
        var rows=em.createQuery("select b from StoredBlobEntity b where b.sha256>:after order by b.sha256",StoredBlobEntity.class)
            .setParameter("after",after).setMaxResults(limit).getResultList();
        var hashes=rows.stream().map(StoredBlobEntity::getSha256).toList();
        var counts=new java.util.HashMap<String,Long>();
        if(!hashes.isEmpty()) em.createQuery("select v.sha256,count(v) from VariantBlobEntity v where v.sha256 in :hashes group by v.sha256",Object[].class)
            .setParameter("hashes",hashes).getResultList().forEach(pair -> counts.put((String)pair[0],(Long)pair[1]));
        var items=new ArrayList<Item>();
        for(var b:rows) {
            long references=counts.getOrDefault(b.getSha256(),0L);
            String state=b.isReady()?"READY":"RESERVED";
            try {physical.readVerified(b.getSha256(),b.getSizeBytes());if(!b.isReady()) state="PRESENT_UNCONFIRMED";}
            catch(IllegalStateException | com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException unavailable) {state=b.isReady()?"MISSING_OR_CORRUPT":"PENDING_WRITE";}
            if(state.equals("READY") && references==0) state="UNREFERENCED_RETAINED";
            items.add(new Item(b.getSha256(),state,references,references>1));
        }
        var unknown=new ArrayList<String>();String nextInventory=null;
        if(includeFilesystem || inventoryCursor!=null) {
            var page=physical.inventory(inventoryCursor,limit);nextInventory=page.nextCursor();
            var physicalHashes=page.filenames().stream().filter(n -> n.matches("[a-f0-9]{64}\\.bin")).map(n -> n.substring(0,64)).toList();
            var known=new java.util.HashSet<String>();
            if(!physicalHashes.isEmpty()) known.addAll(em.createQuery("select b.sha256 from StoredBlobEntity b where b.sha256 in :hashes",String.class).setParameter("hashes",physicalHashes).getResultList());
            for(String name:page.filenames()) if(!name.matches("[a-f0-9]{64}\\.bin") || !known.contains(name.substring(0,64))) unknown.add(name);
        }
        return new Report(List.copyOf(items),List.copyOf(unknown),rows.size()==limit?rows.get(rows.size()-1).getSha256():null,nextInventory);
    }
}
