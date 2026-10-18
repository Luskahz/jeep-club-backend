package com.jeepclub.backend.memberships.core.port;
import com.jeepclub.backend.memberships.core.application.result.MemberImportRow;
import java.util.List;
public interface MemberImportProofPort {
    String issue(Long actor, List<MemberImportRow> rows, String reason);
    void verify(String token, Long actor, List<MemberImportRow> rows, String reason);
}
