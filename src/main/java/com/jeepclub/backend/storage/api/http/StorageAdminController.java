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
@io.swagger.v3.oas.annotations.responses.ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401",description="Autenticação necessária"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403",description="Permissão administrativa insuficiente"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404",description="Mídia ou conteúdo indisponível")
})
public class StorageAdminController {
    private final MediaAdministration admin;private final MediaGovernance governance;private final MediaCatalogRepository catalog;private final BlobInspection inspection;private final com.jeepclub.backend.storage.core.application.MediaResolutionService resolution;private final BlobCursorRegistry cursors;
    public StorageAdminController(MediaAdministration admin,MediaGovernance governance,MediaCatalogRepository catalog,BlobInspection inspection,com.jeepclub.backend.storage.core.application.MediaResolutionService resolution,BlobCursorRegistry cursors) {this.admin=admin;this.governance=governance;this.catalog=catalog;this.inspection=inspection;this.resolution=resolution;this.cursors=cursors;}
    @GetMapping @PreAuthorize("hasAuthority('STORAGE_MEDIA_READ')") @RequiredPermission("STORAGE_MEDIA_READ")
    @io.swagger.v3.oas.annotations.Operation(summary="Pesquisar catálogo de mídias",description="Filtros combináveis por uploader, owner, intervalo de criação [from,before), contexto, MIME, uso, moderação e estado físico. Cursor afterId exclusivo, ordem ascendente e limite máximo 100.")
    public List<MediaAdministration.Entry> list(@RequestParam(defaultValue="0") long afterId,@RequestParam(defaultValue="20") int limit,
        @RequestParam(required=false) MediaContext context,@RequestParam(required=false) String state,
        @RequestParam(required=false) Long uploader,@RequestParam(required=false) Long owner,
        @RequestParam(required=false) Instant createdFrom,@RequestParam(required=false) Instant createdBefore,
        @RequestParam(required=false) String mime,@RequestParam(required=false) MediaAdministration.Usage usage,
        @RequestParam(required=false) String moderation,@RequestParam(required=false) MediaPhysicalState physicalState) {
        return admin.search(new MediaAdministration.Search(afterId,limit,context,state,uploader,owner,createdFrom,createdBefore,mime,usage,moderation,physicalState));
    }
    @GetMapping("/{mediaId}") @PreAuthorize("hasAuthority('STORAGE_MEDIA_READ')") @RequiredPermission("STORAGE_MEDIA_READ")
    @io.swagger.v3.oas.annotations.Operation(summary="Detalhar mídia administrativa",description="Metadados sanitizados, variantes e intervalos de referência; cursores independentes e limite máximo de 100. Não inclui localização física, digest, notas ou dados de identidade.")
    public MediaAdministration.Detail detail(@PathVariable long mediaId,@RequestParam(defaultValue="0") long afterVariantId,
        @RequestParam(defaultValue="0") long afterReferenceId,@RequestParam(defaultValue="20") int limit) {
        return admin.detail(mediaId,afterVariantId,afterReferenceId,limit);
    }
    @GetMapping("/{mediaId}/references") @PreAuthorize("hasAuthority('STORAGE_MEDIA_READ')") @RequiredPermission("STORAGE_MEDIA_READ")
    public List<ReferenceView> references(@PathVariable long mediaId,@RequestParam(defaultValue="0") long afterId,@RequestParam(defaultValue="20") int limit) {
        return catalog.findReferences(mediaId,afterId,limit).stream().map(r -> new ReferenceView(r.referenceId(),r.context(),r.role(),r.linkedAt(),r.unlinkedAt())).toList();
    }
    public record ReferenceView(long referenceId,MediaContext context,String role,Instant linkedAt,Instant unlinkedAt) {}
    @GetMapping("/{mediaId}/audit") @PreAuthorize("hasAuthority('STORAGE_MEDIA_AUDIT_READ')") @RequiredPermission("STORAGE_MEDIA_AUDIT_READ")
    public List<MediaAdministration.Event> audit(@PathVariable long mediaId,@RequestParam(defaultValue="0") long afterId,@RequestParam(defaultValue="20") int limit) { return admin.events(mediaId,afterId,limit); }
    @GetMapping("/{mediaId}/references/{referenceId}/preview/{code}") @PreAuthorize("hasAuthority('STORAGE_MEDIA_PREVIEW')") @RequiredPermission("STORAGE_MEDIA_PREVIEW")
    public org.springframework.http.ResponseEntity<byte[]> preview(@PathVariable long mediaId,@PathVariable long referenceId,@PathVariable String code,
        @RequestParam MediaGovernance.PreviewPurpose purpose,@AuthenticationPrincipal UserPrincipal actor) {
        com.jeepclub.backend.storage.core.application.MediaResolutionService.Content content;
        try { content=resolution.resolve(actor.getUserId(),mediaId,referenceId,code); }
        catch (org.springframework.security.access.AccessDeniedException denied) { throw new com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException(); }
        governance.previewAccess(mediaId,referenceId,purpose,audit(actor));
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
    public record HoldChange(Boolean held,String justification) {}
    @PostMapping("/{mediaId}/legal-hold") @PreAuthorize("hasAuthority('STORAGE_MEDIA_LEGAL_HOLD')") @RequiredPermission("STORAGE_MEDIA_LEGAL_HOLD")
    public void hold(@PathVariable long mediaId,@RequestBody HoldChange change,@AuthenticationPrincipal UserPrincipal actor) {if(change.held()==null) throw new IllegalArgumentException("Hold state required");governance.legalHold(mediaId,change.held(),change.justification(),audit(actor));}
    @PostMapping("/{mediaId}/retention-review") @PreAuthorize("hasAuthority('STORAGE_MEDIA_RETENTION_REVIEW')") @RequiredPermission("STORAGE_MEDIA_RETENTION_REVIEW")
    public MediaAdministration.RetentionReview review(@PathVariable long mediaId,@AuthenticationPrincipal UserPrincipal actor) {return admin.review(mediaId,audit(actor));}
    @PostMapping("/{mediaId}/discard") @PreAuthorize("hasAuthority('STORAGE_MEDIA_RETENTION_REVIEW')") @RequiredPermission("STORAGE_MEDIA_RETENTION_REVIEW")
    public void discard(@PathVariable long mediaId,@AuthenticationPrincipal UserPrincipal actor) {admin.discard(mediaId,audit(actor));}
    public record InventorySummary(int checked,int shared,int inconsistent,int uncatalogued,String nextBlobCursor,String nextInventoryCursor) {}
    @PostMapping("/reconciliation") @PreAuthorize("hasAuthority('STORAGE_MEDIA_RECONCILE')") @RequiredPermission("STORAGE_MEDIA_RECONCILE")
    public InventorySummary reconcile(@RequestParam(required=false) String blobCursor,@RequestParam(defaultValue="20") int limit,@RequestParam(required=false) String inventoryCursor,@RequestParam(defaultValue="false") boolean includeFilesystem,@AuthenticationPrincipal UserPrincipal actor) {
        if(actor==null) throw new org.springframework.security.access.AccessDeniedException("Permission required");
        var report=inspection.inspect(cursors.resolve(actor.getUserId(),blobCursor),limit,inventoryCursor,includeFilesystem);
        return new InventorySummary(report.items().size(),(int)report.items().stream().filter(BlobInspection.Item::shared).count(),
            (int)report.items().stream().filter(i -> !i.state().equals("READY") && !i.state().equals("UNREFERENCED_RETAINED")).count(),report.uncataloguedFiles().size(),cursors.issue(actor.getUserId(),report.nextBlobCursor()),report.nextInventoryCursor());
    }
    private MediaAuditContext audit(UserPrincipal actor) {return new MediaAuditContext(MediaActorKind.USER,actor.getUserId(),Instant.now(),null);}
}
