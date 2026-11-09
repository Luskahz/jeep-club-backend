package com.jeepclub.backend.storage.infra.persistence.adapter;

import com.jeepclub.backend.storage.core.repository.*;
import com.jeepclub.backend.storage.infra.persistence.entity.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/** Reserve custody before IO. A failed logical upload leaves a known, retained blob, never a dangling reference. */
@Repository
public class BlobCatalogAdapter implements BlobCatalog {
    private final EntityManager em;
    private final PhysicalBlobStore physical;
    private final TransactionTemplate isolated;
    public BlobCatalogAdapter(EntityManager em, PhysicalBlobStore physical, PlatformTransactionManager manager) {
        this.em=em; this.physical=physical; isolated=new TransactionTemplate(manager);
        isolated.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    public static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @Override public Blob store(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > 10*1024*1024) throw new IllegalArgumentException("Invalid blob size");
        byte[] snapshot=bytes.clone(); String hash=digest(snapshot);
        try {
            isolated.executeWithoutResult(status -> {
                if (em.find(StoredBlobEntity.class, hash) != null) return;
                var row=new StoredBlobEntity(); row.setSha256(hash); row.setSizeBytes(snapshot.length);
                row.setCreatedAt(Instant.now()); row.setReady(false); em.persist(row); em.flush();
            });
        } catch (RuntimeException collision) {
            // The losing insert transaction has ended. Verify the winning reservation in a fresh transaction.
            if (!Boolean.TRUE.equals(isolated.execute(status -> em.find(StoredBlobEntity.class, hash) != null))) throw collision;
        }
        return isolated.execute(status -> {
            var row=em.find(StoredBlobEntity.class, hash, LockModeType.PESSIMISTIC_WRITE);
            if (row.getSizeBytes()!=snapshot.length) throw new IllegalStateException("Blob integrity mismatch");
            physical.putVerified(hash, snapshot); // Also checks READY objects: missing/corrupt files cannot be silently reused.
            row.setReady(true); em.flush();
            return new Blob(hash, row.getSizeBytes(), "storage-blobs/"+hash+".bin");
        });
    }
    @Override @Transactional public void bind(Long variantId, Blob blob) {
        var row=em.find(StoredBlobEntity.class, blob.sha256(), LockModeType.PESSIMISTIC_READ);
        var variant=em.find(MediaVariantEntity.class, variantId);
        if (row==null || !row.isReady() || variant==null || row.getSizeBytes()!=variant.getSizeBytes()
            || blob.sizeBytes()!=row.getSizeBytes() || !("storage-blobs/"+blob.sha256()+".bin").equals(blob.storageKey())
            || !blob.storageKey().equals(variant.getStorageKey()) || !blob.sha256().equals(variant.getChecksum())
            || !"SHA-256".equals(variant.getChecksumAlgorithm()) || !"LOCAL".equals(variant.getProvider())
            || !"storage-blobs".equals(variant.getNamespace())) throw new IllegalArgumentException("Invalid blob binding");
        var binding=new VariantBlobEntity(); binding.setVariantId(variantId); binding.setSha256(blob.sha256());
        em.persist(binding); em.flush();
    }
    @Override @Transactional(readOnly=true) public Optional<Blob> findByVariant(Long variantId) {
        var binding=em.find(VariantBlobEntity.class, variantId);
        if (binding==null) return Optional.empty();
        var row=em.find(StoredBlobEntity.class,binding.getSha256());
        if (row==null || !row.isReady()) return Optional.empty();
        return Optional.of(new Blob(row.getSha256(),row.getSizeBytes(),"storage-blobs/"+row.getSha256()+".bin"));
    }
    @Override public byte[] read(Blob blob) { return physical.readVerified(blob.sha256(),blob.sizeBytes()); }
}
