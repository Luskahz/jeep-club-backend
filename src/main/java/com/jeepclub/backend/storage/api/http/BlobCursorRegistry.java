package com.jeepclub.backend.storage.api.http;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Bounded per-instance opaque cursors. Never place a digest in the HTTP token. */
@Component
final class BlobCursorRegistry {
    private record Cursor(long actor,String digest,long expires) {}
    private final Map<String,Cursor> cursors=new HashMap<>();
    private void expire(long now) { cursors.values().removeIf(c -> now-c.expires()>=0); }
    synchronized String resolve(long actor,String token) {
        if (actor<=0) throw new IllegalArgumentException("Invalid cursor actor");
        if (token==null) return "";
        long now=System.nanoTime();expire(now);
        var cursor=cursors.get(token);
        if (cursor==null || cursor.actor()!=actor) throw new IllegalArgumentException("Invalid or expired cursor");
        return cursor.digest();
    }
    synchronized String issue(long actor,String digest) {
        if (digest==null) return null;
        if (actor<=0 || !digest.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid cursor");
        long now=System.nanoTime();expire(now);
        if (cursors.size()>=1024) throw new IllegalStateException("Cursor capacity exceeded");
        String token=UUID.randomUUID().toString();
        cursors.put(token,new Cursor(actor,digest,now+TimeUnit.MINUTES.toNanos(2)));
        return token;
    }
}
