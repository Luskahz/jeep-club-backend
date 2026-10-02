package com.jeepclub.backend.platform.export;
import java.io.*;
import java.util.List;
final class CsvExportWriter {
    private CsvExportWriter() {}
    static void line(Writer writer, List<String> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) writer.write(';');
            String cell = cells.get(i) == null ? "" : cells.get(i);
            String inspected = cell.stripLeading();
            if (!inspected.isEmpty() && "=+-@".indexOf(inspected.charAt(0)) >= 0
                    || !cell.isEmpty() && "\t\r\n".indexOf(cell.charAt(0)) >= 0) cell = "'" + cell;
            writer.write('"'); writer.write(cell.replace("\"", "\"\"")); writer.write('"');
        }
        writer.write("\r\n");
    }
}
