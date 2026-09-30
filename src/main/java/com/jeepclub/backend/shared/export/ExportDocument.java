package com.jeepclub.backend.shared.export;
import java.util.List;
import java.util.function.Consumer;
public record ExportDocument(String filename, String title, List<String> columns,
                             List<String> filters, Rows rows) {
    public ExportDocument { columns = List.copyOf(columns); filters = List.copyOf(filters); }
    @FunctionalInterface public interface Rows { void read(Consumer<ExportRow> consumer); }
}
