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
        boolean posix=posix();
        if(posix) Files.createDirectories(root,java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));
        else Files.createDirectories(root);
        if(posix) Files.setPosixFilePermissions(root,java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        for (Path p=root; p!=null; p=p.getParent()) if (Files.isSymbolicLink(p)) throw new IOException("Symbolic storage path");
    }
    private boolean posix() throws IOException {
        Path existing=root;while(!Files.exists(existing,LinkOption.NOFOLLOW_LINKS)) existing=existing.getParent();
        return Files.getFileStore(existing).supportsFileAttributeView(java.nio.file.attribute.PosixFileAttributeView.class);
    }
    private java.nio.file.attribute.FileAttribute<?>[] fileAttributes() throws IOException {
        return posix()?new java.nio.file.attribute.FileAttribute<?>[]{java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"))}:new java.nio.file.attribute.FileAttribute<?>[0];
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
            try (var channel=FileChannel.open(stage,Set.of(StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS),fileAttributes())) {
                ByteBuffer buffer=ByteBuffer.wrap(bytes); while(buffer.hasRemaining()) channel.write(buffer); channel.force(true);
            }
            Files.move(stage,target,StandardCopyOption.ATOMIC_MOVE);
            readVerified(digest,bytes.length);
        } catch (IOException e) { throw new IllegalStateException("Physical blob write failed; custody preserved for reconciliation",e); }
    }
    @Override public byte[] readVerified(String digest,long size) {
        try {
            safe(); Path target=path(digest);
            var attributes=Files.readAttributes(target,java.nio.file.attribute.BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
            if (size<=0 || size>10*1024*1024 || !attributes.isRegularFile() || attributes.size()!=size)
                throw new IOException("Blob unavailable or inconsistent");
            if(posix()) Files.setPosixFilePermissions(target,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            try(var input=Files.newInputStream(target,LinkOption.NOFOLLOW_LINKS)) {
                byte[] bytes=input.readNBytes((int)size+1);
                if (bytes.length!=size || !BlobCatalogAdapter.digest(bytes).equals(digest)) throw new IOException("Blob integrity mismatch");
                return bytes;
            }
        } catch(NoSuchFileException missing) {throw new com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException();}
        catch(IOException e) { throw new IllegalStateException("Physical blob verification failed",e); }
    }
    private static final class Scan {
        final DirectoryStream<Path> directory;final java.util.Iterator<Path> iterator;final long expires;
        Scan(DirectoryStream<Path> directory) {this.directory=directory;this.iterator=directory.iterator();expires=System.nanoTime()+java.util.concurrent.TimeUnit.MINUTES.toNanos(2);}
    }
    private final java.util.Map<String,Scan> scans=new java.util.HashMap<>();
    @Override public synchronized InventoryPage inventory(String cursor,int limit) {
        if(limit<1 || limit>100) throw new IllegalArgumentException("Invalid inventory page size");
        var expired=scans.entrySet().iterator();
        while(expired.hasNext()) {var e=expired.next();if(System.nanoTime()-e.getValue().expires>=0) {close(e.getValue());expired.remove();}}
        try {
            safe();Scan scan;
            if(cursor==null) {
                if(scans.size()>=8) throw new IllegalStateException("Too many active inventories");
                cursor=UUID.randomUUID().toString();scan=new Scan(Files.newDirectoryStream(root));scans.put(cursor,scan);
            } else {scan=scans.get(cursor);if(scan==null) throw new IllegalArgumentException("Expired or invalid inventory cursor");}
            var names=new java.util.ArrayList<String>();
            try {
                while(names.size()<limit && scan.iterator.hasNext()) names.add(scan.iterator.next().getFileName().toString());
                if(!scan.iterator.hasNext()) {scans.remove(cursor);close(scan);cursor=null;}
                return new InventoryPage(names,cursor);
            } catch(RuntimeException failure) {scans.values().remove(scan);close(scan);throw failure;}
        } catch(IOException e) {throw new IllegalStateException("Physical inventory failed",e);}
    }
    private void close(Scan scan) {try {scan.directory.close();} catch(IOException failure) {throw new IllegalStateException("Inventory close failed",failure);}}
    @jakarta.annotation.PreDestroy public synchronized void closeInventories() {for(var scan:scans.values()) close(scan);scans.clear();}
}
