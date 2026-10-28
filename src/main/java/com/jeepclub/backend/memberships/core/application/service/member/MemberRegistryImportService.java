package com.jeepclub.backend.memberships.core.application.service.member;
import com.jeepclub.backend.memberships.core.application.result.*;
import com.jeepclub.backend.memberships.core.domain.model.MembershipNumber;
import com.jeepclub.backend.memberships.core.domain.exception.MemberRegistryException;
import com.jeepclub.backend.memberships.core.port.*;
import com.jeepclub.backend.memberships.core.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Service @RequiredArgsConstructor
public class MemberRegistryImportService {
    private final MemberRepository members;
    private final MemberIdentityPort identities;
    private final MemberAuthenticationEligibility authentication;
    private final MemberImportProofPort proof;
    private final AdminMemberService admin;
    private final MemberImportApplicationPolicy policy;
    @Transactional(readOnly=true)
    public MemberImportReport dryRun(List<MemberImportRow> rows,Long actor,String reason) {
        validate(rows,actor,reason);
        return new MemberImportReport(proof.issue(actor,rows,reason), preview(rows));
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public MemberImportReport apply(List<MemberImportRow> rows,Long actor,String reason,String token) {
        if(!policy.applyEnabled()) throw new MemberRegistryException(MemberRegistryException.Kind.IMPORT_DISABLED);
        validate(rows,actor,reason);proof.verify(token,actor,rows,reason);
        var preview=preview(rows);var result=new ArrayList<MemberImportReport.Row>();
        for(var entry:preview) {
            if(!entry.outcome().equals("READY") && !entry.outcome().equals("ALREADY_LINKED")) { result.add(entry); continue; }
            var row=rows.get(entry.index());
            try {
                // No importer transaction: each call crosses the transactional service proxy.
                admin.assign(row.identityUserId(),row.membershipNumber(),actor,"IMPORT "+row.sourceReference()+": "+reason);
                result.add(new MemberImportReport.Row(entry.index(),entry.outcome().equals("ALREADY_LINKED")?"ALREADY_LINKED":"APPLIED"));
            } catch(MemberRegistryException conflict) {
                result.add(new MemberImportReport.Row(entry.index(),conflict.getKind().name()));
            } catch(RuntimeException failure) {
                result.add(new MemberImportReport.Row(entry.index(),"FAILED"));
            }
        }
        return new MemberImportReport(null,List.copyOf(result));
    }
    private List<MemberImportReport.Row> preview(List<MemberImportRow> rows) {
        Map<Long,Integer> ids=new HashMap<>();Map<String,Integer> numbers=new HashMap<>();
        for(var row:rows) { ids.merge(row.identityUserId(),1,Integer::sum);numbers.merge(new MembershipNumber(row.membershipNumber()).claimKey(),1,Integer::sum); }
        var result=new ArrayList<MemberImportReport.Row>();
        for(int i=0;i<rows.size();i++) {
            var r=rows.get(i);var key=new MembershipNumber(r.membershipNumber()).claimKey();String outcome;
            if(!r.administrativelyConfirmed()) outcome="UNVERIFIED";
            else if(ids.get(r.identityUserId())>1 || numbers.get(key)>1) outcome="DUPLICATE_SOURCE";
            else if(!identities.exists(r.identityUserId())) outcome="IDENTITY_NOT_FOUND";
            else if(!identities.isActive(r.identityUserId())) outcome="IDENTITY_INACTIVE";
            else {
                var current=members.byIdentity(r.identityUserId());
                outcome=current.isPresent() ? (current.get().membershipNumber().equals(new MembershipNumber(r.membershipNumber()).value())?"ALREADY_LINKED":"IDENTITY_ALREADY_LINKED")
                        : !authentication.hasCompletedFirstAccess(r.identityUserId())?"FIRST_ACCESS_INCOMPLETE"
                        : availability(r,key);
            }
            result.add(new MemberImportReport.Row(i,outcome));
        }
        return List.copyOf(result);
    }
    private String availability(MemberImportRow row,String key) {
        if(!members.claimed(key)) return "READY";
        // Apply preview is nontransactional: a concurrent assign may commit between the
        // first Member read and the claim read. Recognize that same binding as idempotent;
        // assign still revalidates under Identity/account/Registry locks before any write.
        return members.byIdentity(row.identityUserId())
                .filter(member -> member.membershipNumber().equals(new MembershipNumber(row.membershipNumber()).value()))
                .map(member -> "ALREADY_LINKED").orElse("NUMBER_UNAVAILABLE");
    }
    private void validate(List<MemberImportRow> rows,Long actor,String reason) {
        AdminMemberService.validateActorReason(actor,reason);
        if(reason.length()>300 || rows==null || rows.isEmpty() || rows.size()>500) throw invalid();
        for(var r:rows) {
            if(r==null || r.identityUserId()==null || r.identityUserId()<=0 || r.sourceReference()==null
                || r.sourceReference().isBlank() || r.sourceReference().length()>100
                || !r.sourceReference().matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,99}")) throw invalid();
            new MembershipNumber(r.membershipNumber());
        }
    }
    private MemberRegistryException invalid() { return new MemberRegistryException(MemberRegistryException.Kind.INVALID); }
}
