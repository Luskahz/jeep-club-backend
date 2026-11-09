package com.jeepclub.backend.storage.api.http;

import com.jeepclub.backend.storage.core.repository.*;
import com.jeepclub.backend.storage.core.domain.model.*;
import com.jeepclub.backend.storage.core.domain.enums.*;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController @RequestMapping("/admin/storage/media")
@ConditionalOnProperty(name="storage.media.enabled",havingValue="true")
public class StorageAdminController {
    private final MediaAdministration admin;private final MediaGovernance governance;private final MediaCatalogRepository catalog;private final BlobInspection inspection;private final com.jeepclub.backend.storage.core.application.MediaResolutionService resolution;
    public StorageAdminController(MediaAdministration admin,MediaGovernance governance,MediaCatalogRepository catalog,BlobInspection inspection,com.jeepclub.backend.storage.core.application.MediaResolutionService resolution) {this.admin=admin;this.governance=governance;this.catalog=catalog;this.inspection=inspection;this.resolution=resolution;}
    @GetMapping @PreAuthorize("hasAuthority('STORAGE_MEDIA_READ')") @RequiredPermission("STORAGE_MEDIA_READ")
    public List<MediaAdministration.Entry> list(@RequestParam(defaultValue="0") long afterId,@RequestParam(defaultValue="20") int limit,
        @RequestParam(required=false) MediaContext context,@RequestParam(required=false) String state) { return admin.list(afterId,limit,context,state); }
    @GetMapping("/{mediaId}/references") @PreAuthorize("hasAuthority('STORAGE_MEDIA_READ')") @RequiredPermission("STORAGE_MEDIA_READ")
    public List<ReferenceView> references(@PathVariable long mediaId,@RequestParam(defaultValue="0") long afterId,@RequestParam(defaultValue="20") int limit) {
        return catalog.findReferences(mediaId,afterId,limit).stream().map(r -> new ReferenceView(r.referenceId(),r.context(),r.consumerId(),r.role(),r.linkedAt(),r.unlinkedAt())).toList();
    }
    public record ReferenceView(long referenceId,MediaContext context,String consumerId,String role,Instant linkedAt,Instant unlinkedAt) {}
    @GetMapping("/{mediaId}/audit") @PreAuthorize("hasAuthority('STORAGE_MEDIA_AUDIT_READ')") @RequiredPermission("STORAGE_MEDIA_AUDIT_READ")
    public List<MediaAdministration.Event> audit(@PathVariable long mediaId,@RequestParam(defaultValue="0") long afterId,@RequestParam(defaultValue="20") int limit) { return admin.events(mediaId,afterId,limit); }
    @GetMapping("/{mediaId}/references/{referenceId}/preview/{code}") @PreAuthorize("hasAuthority('STORAGE_MEDIA_READ')") @RequiredPermission("STORAGE_MEDIA_READ")
    public org.springframework.http.ResponseEntity<byte[]> preview(@PathVariable long mediaId,@PathVariable long referenceId,@PathVariable String code,@AuthenticationPrincipal UserPrincipal actor) {
        var content=resolution.resolve(actor.getUserId(),mediaId,referenceId,code);
        governance.previewAccess(mediaId,referenceId,audit(actor));
        return org.springframework.http.ResponseEntity.ok().header("Cache-Control","private, no-store").contentType(org.springframework.http.MediaType.parseMediaType(content.mimeType())).body(content.bytes());
    }
    public record Moderate(Long referenceId,ModerationDecision.State state,ModerationDecision.Reason reason,String internalNote) {}
    @PostMapping("/{mediaId}/moderation") @PreAuthorize("hasAuthority('STORAGE_MEDIA_MODERATE')") @RequiredPermission("STORAGE_MEDIA_MODERATE")
    public void moderate(@PathVariable long mediaId,@RequestBody Moderate request,@AuthenticationPrincipal UserPrincipal actor) {
        if(request.state()==null || request.reason()==null) throw new IllegalArgumentException("Missing moderation decision");
        if(request.state()==ModerationDecision.State.VISIBLE) throw new IllegalArgumentException("Use restoration endpoint");
        governance.moderate(new ModerationDecision(mediaId,request.referenceId(),request.state(),request.reason(),request.internalNote(),actor.getUserId(),Instant.now()));
    }
    @PostMapping("/{mediaId}/restore") @PreAuthorize("hasAuthority('STORAGE_MEDIA_RESTORE')") @RequiredPermission("STORAGE_MEDIA_RESTORE")
    public void restore(@PathVariable long mediaId,@RequestParam(required=false) Long referenceId,@AuthenticationPrincipal UserPrincipal actor) {
        governance.moderate(new ModerationDecision(mediaId,referenceId,ModerationDecision.State.VISIBLE,ModerationDecision.Reason.RESTORED,null,actor.getUserId(),Instant.now()));
    }
    @PostMapping("/{mediaId}/legal-hold") @PreAuthorize("hasAuthority('STORAGE_MEDIA_LEGAL_HOLD')") @RequiredPermission("STORAGE_MEDIA_LEGAL_HOLD")
    public void hold(@PathVariable long mediaId,@RequestParam boolean held,@AuthenticationPrincipal UserPrincipal actor) {governance.legalHold(mediaId,held,audit(actor));}
    @PostMapping("/{mediaId}/retention-review") @PreAuthorize("hasAuthority('STORAGE_MEDIA_RETENTION_REVIEW')") @RequiredPermission("STORAGE_MEDIA_RETENTION_REVIEW")
    public MediaAdministration.RetentionReview review(@PathVariable long mediaId,@AuthenticationPrincipal UserPrincipal actor) {return admin.review(mediaId,audit(actor));}
    @PostMapping("/{mediaId}/discard") @PreAuthorize("hasAuthority('STORAGE_MEDIA_RETENTION_REVIEW')") @RequiredPermission("STORAGE_MEDIA_RETENTION_REVIEW")
    public void discard(@PathVariable long mediaId,@AuthenticationPrincipal UserPrincipal actor) {admin.discard(mediaId,audit(actor));}
    public record InventorySummary(int checked,int shared,int inconsistent,int uncatalogued) {}
    @PostMapping("/reconciliation") @PreAuthorize("hasAuthority('STORAGE_MEDIA_RECONCILE')") @RequiredPermission("STORAGE_MEDIA_RECONCILE")
    public InventorySummary reconcile(@RequestParam(defaultValue="") String afterDigest,@RequestParam(defaultValue="20") int limit) {
        var report=inspection.inspect(afterDigest,limit);
        return new InventorySummary(report.items().size(),(int)report.items().stream().filter(BlobInspection.Item::shared).count(),
            (int)report.items().stream().filter(i -> !i.state().equals("READY") && !i.state().equals("UNREFERENCED_RETAINED")).count(),report.uncataloguedFiles().size());
    }
    private MediaAuditContext audit(UserPrincipal actor) {return new MediaAuditContext(MediaActorKind.USER,actor.getUserId(),Instant.now(),null);}
}
