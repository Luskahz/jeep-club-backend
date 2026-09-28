package com.jeepclub.backend.platform.export;
import com.jeepclub.backend.shared.export.*;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
@Component
public class DefaultExportRenderer implements ExportRenderer {
    private final Clock clock;
    private final int csvLimit, pdfLimit, byteLimit;
    public DefaultExportRenderer(Clock clock,
            @Value("${exports.csv-max-rows:20000}") int csvLimit,
            @Value("${exports.pdf-max-rows:500}") int pdfLimit,
            @Value("${exports.max-bytes:16777216}") int byteLimit) {
        this.clock=clock; this.csvLimit=csvLimit; this.pdfLimit=pdfLimit; this.byteLimit=byteLimit;
    }
    public ExportFile render(ExportDocument document, ExportFormat format) {
        if (format == null || !document.filename().matches("[a-z][a-z0-9-]{0,79}"))
            throw new ExportException(ExportException.Reason.INVALID_FILTER);
        Instant generated = clock.instant();
        var output = new LimitedOutput(byteLimit);
        int[] count = {0};
        try {
            if (format == ExportFormat.CSV) {
                var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
                writer.write('\uFEFF'); CsvExportWriter.line(writer, document.columns());
                document.rows().read(row -> {
                    check(row, document, ++count[0], csvLimit);
                    try { CsvExportWriter.line(writer,row.cells()); writer.flush(); }
                    catch (IOException e) { throw new UncheckedIOException(e); }
                });
                writer.flush();
            } else {
                try (var pdf = new PdfExportWriter(document, generated)) {
                    document.rows().read(row -> {
                        check(row, document, ++count[0], pdfLimit);
                        try { pdf.row(row); } catch (IOException e) { throw new UncheckedIOException(e); }
                    });
                    pdf.save(output);
                }
            }
        } catch (IOException | UncheckedIOException e) {
            throw new ExportException(ExportException.Reason.GENERATION);
        }
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC).format(generated);
        return new ExportFile(document.filename()+"-"+stamp+"."+format.name().toLowerCase(Locale.ROOT),
            format == ExportFormat.CSV ? "text/csv;charset=UTF-8" : "application/pdf", output.toByteArray());
    }
    private static void check(ExportRow row, ExportDocument doc, int count, int limit) {
        if (count > limit) throw new ExportException(ExportException.Reason.LIMIT);
        if (row.cells().size() != doc.columns().size()) throw new ExportException(ExportException.Reason.GENERATION);
    }
    private static final class LimitedOutput extends ByteArrayOutputStream {
        private final int limit;
        LimitedOutput(int limit) { this.limit=limit; }
        private void check(int length) { if ((long) count+length>limit) throw new ExportException(ExportException.Reason.LIMIT); }
        @Override public synchronized void write(int b) { check(1); super.write(b); }
        @Override public synchronized void write(byte[] b,int off,int len) { check(len); super.write(b,off,len); }
    }
}
