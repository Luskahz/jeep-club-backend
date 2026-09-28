package com.jeepclub.backend.shared.export;
import java.util.List;
/** A group is a reader-facing heading, never a technical identifier or serialized object. */
public record ExportRow(String group, List<String> cells) {
    public ExportRow { cells = List.copyOf(cells); }
    public static ExportRow of(Object... cells) {
        return new ExportRow(null, java.util.Arrays.stream(cells).map(ExportValues::text).toList());
    }
    public ExportRow grouped(String heading) { return new ExportRow(heading, cells); }
}
