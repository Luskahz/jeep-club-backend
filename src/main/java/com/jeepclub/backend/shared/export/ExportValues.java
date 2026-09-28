package com.jeepclub.backend.shared.export;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
public final class ExportValues {
    private ExportValues() {}
    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss XXX").withZone(ZONE);
    public static String text(Object value) {
        if (value == null) return "";
        if (value instanceof Boolean b) return b ? "Sim" : "Não";
        if (value instanceof Instant i) return TIME.format(i);
        if (value instanceof LocalDateTime t) return TIME.format(t.toInstant(ZoneOffset.UTC));
        if (value instanceof LocalDate d) return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        if (value instanceof BigDecimal d) return d.toPlainString().replace('.', ',');
        return value.toString();
    }
}
