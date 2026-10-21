package com.jeepclub.backend.memberships.core.application.result;
import java.util.List;
public record MemberImportReport(String dryRunToken, List<Row> rows) {
    public record Row(int index, String outcome) { }
}
