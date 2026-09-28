package com.jeepclub.backend.shared.export;
import java.util.List;
import java.util.function.*;
/** Reads one bounded chunk at a time; callers own queries, mapping and the transaction. */
public final class ExportPages {
    public static final int CHUNK = 200;
    private ExportPages() {}
    public static <T> void read(IntFunction<List<T>> page, Consumer<T> consumer) {
        for (int offset = 0; ; offset += CHUNK) {
            List<T> batch = page.apply(offset);
            batch.forEach(consumer);
            if (batch.size() < CHUNK) return;
        }
    }
}
