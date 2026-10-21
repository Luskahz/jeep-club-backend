package com.jeepclub.backend.memberships.api.http.dto.member;
import com.jeepclub.backend.memberships.core.application.result.MemberImportReport;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
@Schema(name="MemberRegistryImportReport",description="Relatório sanitizado. Índices referem-se às linhas do request, sem dados pessoais.")
public record MemberImportReportDTO(@Schema(description="Comprovante temporário da simulação; null na aplicação.") String dryRunToken,List<Row> rows) {
    public record Row(int index,String outcome) { }
    public static MemberImportReportDTO from(MemberImportReport result) {
        return new MemberImportReportDTO(result.dryRunToken(),result.rows().stream().map(r -> new Row(r.index(),r.outcome())).toList());
    }
}
