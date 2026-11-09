package com.jeepclub.backend.storage.infra.blob;

import com.jeepclub.backend.storage.core.repository.PhysicalBlobStore;
import com.jeepclub.backend.storage.infra.persistence.adapter.BlobCatalogAdapter;
import com.jeepclub.backend.platform.storage.properties.StorageProperties;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Same configured provider root, separate private namespace. No delete or overwrite operations. */
@Component
public class LocalPhysicalBlobStore implements PhysicalBlobStore {
    private final Path root;
    public LocalPhysicalBlobStore(StorageProperties properties) { root=properties.local().rootDirectory().toAbsolutePath().normalize().resolve("storage-blobs"); }
    private void safe() throws IOException {
        for (Path p=root; p!=null; p=p.getParent()) if (Files.isSymbolicLink(p)) throw new IOException("Symbolic storage path");
        Files.createDirectories(root);
        for (Path p=root; p!=null; p=p.getParent()) if (Files.isSymbolicLink(p)) throw new IOException("Symbolic storage path");
    }
    private Path path(String digest) {
        if (digest==null || !digest.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid digest");
        return root.resolve(digest+".bin");
    }
    @Override public void putVerified(String digest, byte[] bytes) {
        if (!BlobCatalogAdapter.digest(bytes).equals(digest)) throw new IllegalArgumentException("Digest mismatch");
        try {
            safe(); Path target=path(digest);
            if (Files.exists(target,LinkOption.NOFOLLOW_LINKS)) { readVerified(digest,bytes.length); return; }
            Path stage=root.resolve(digest+"."+UUID.randomUUID()+".pending");
            try (var channel=FileChannel.open(stage,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer buffer=ByteBuffer.wrap(bytes); while(buffer.hasRemaining()) channel.write(buffer); channel.force(true);
            }
            Files.move(stage,target,StandardCopyOption.ATOMIC_MOVE);
            readVerified(digest,bytes.length);
        } catch (IOException e) { throw new IllegalStateException("Physical blob write failed; custody preserved for reconciliation",e); }
    }
    @Override public byte[] readVerified(String digest,long size) {
        try {
            safe(); Path target=path(digest);
            if (size<=0 || size>10*1024*1024 || !Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS) || Files.size(target)!=size)
                throw new IOException("Blob unavailable or inconsistent");
            try(var input=Files.newInputStream(target,LinkOption.NOFOLLOW_LINKS)) {
                byte[] bytes=input.readNBytes((int)size+1);
                if (bytes.length!=size || !BlobCatalogAdapter.digest(bytes).equals(digest)) throw new IOException("Blob integrity mismatch");
                return bytes;
            }
        } catch(IOException e) { throw new IllegalStateException("Physical blob verification failed",e); }
    }
    @Override public Set<String> inventory() {
        try { safe(); try(var paths=Files.list(root)) { return paths.map(p -> p.getFileName().toString()).collect(Collectors.toUnmodifiableSet()); } }
        catch(IOException e) { throw new IllegalStateException("Physical inventory failed",e); }
    }
}
