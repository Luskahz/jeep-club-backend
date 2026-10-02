package com.jeepclub.backend.publications.api.http.controller.member;

import com.jeepclub.backend.memberships.api.security.RequiresMembership;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.publications.core.application.service.PublicationCommentService;
import com.jeepclub.backend.publications.core.application.service.PublicationLikeService;
import com.jeepclub.backend.publications.core.domain.model.PublicationComment;
import com.jeepclub.backend.publications.core.domain.model.PublicationCommentImage;
import com.jeepclub.backend.publications.core.domain.model.PublicationLike;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/publications/{publicationId}")
@RequiredArgsConstructor
@RequiresMembership
@Tag(name = "Publication social", description = "Interactions on published Notice, Event and Service publications")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid comment or image key", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Membership or permission required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Publication inaccessible or image missing", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Concurrent interaction conflict", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class PublicationSocialController {
    private final PublicationLikeService likes;
    private final PublicationCommentService comments;

    public record LikeResponse(Long id, Long publicationId, Instant createdAt) {
        static LikeResponse from(PublicationLike like) { return new LikeResponse(like.id(), like.publicationId(), like.createdAt()); }
    }
    public record CommentImage(String storageKey, int position) {}
    public record CommentRequest(String text, List<CommentImage> images) {}
    public record CommentResponse(Long id, Long publicationId, Long authorUserId, String text,
                                  List<CommentImage> images, Instant createdAt) {
        static CommentResponse from(PublicationComment comment) {
            return new CommentResponse(comment.getId(), comment.getPublicationId(), comment.getAuthorUserId(),
                    comment.getContent(), comment.getImages().stream()
                    .map(i -> new CommentImage(i.storageKey(), i.position())).toList(), comment.getCreatedAt());
        }
    }

    @PostMapping("/likes")
    @PreAuthorize("hasAuthority('PUBLICATIONS_INTERACTION_LIKE')")
    @RequiredPermission("PUBLICATIONS_INTERACTION_LIKE")
    @Operation(summary = "Like a published publication", description = "Idempotent for the authenticated member. Event FINISHED remains available when its Publication is PUBLISHED.")
    @ApiResponse(responseCode = "200", description = "Like exists, newly created or previously present")
    public LikeResponse like(@PathVariable Long publicationId, @AuthenticationPrincipal UserPrincipal principal) {
        return LikeResponse.from(likes.like(publicationId, principal.getUserId()));
    }

    @DeleteMapping("/likes/me")
    @PreAuthorize("hasAuthority('PUBLICATIONS_INTERACTION_LIKE')")
    @RequiredPermission("PUBLICATIONS_INTERACTION_LIKE")
    @Operation(summary = "Remove my like", description = "Idempotent; only the authenticated member's like is removed.")
    @ApiResponse(responseCode = "204", description = "My like is absent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(@PathVariable Long publicationId, @AuthenticationPrincipal UserPrincipal principal) {
        likes.unlike(publicationId, principal.getUserId());
    }

    @PostMapping("/comments")
    @PreAuthorize("hasAuthority('PUBLICATIONS_COMMENT_CREATE')")
    @RequiredPermission("PUBLICATIONS_COMMENT_CREATE")
    @Operation(summary = "Comment on a published publication", description = "Text, global image storage keys, or both are required; an empty comment is rejected.")
    @ApiResponse(responseCode = "201", description = "Comment created")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse comment(@PathVariable Long publicationId, @RequestBody CommentRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        if (request == null || (request.images() != null && request.images().stream().anyMatch(java.util.Objects::isNull))) {
            throw new IllegalArgumentException("Invalid comment payload.");
        }
        var gallery = request.images() == null ? List.<PublicationCommentImage>of() : request.images().stream()
                .map(i -> new PublicationCommentImage(i.storageKey(), i.position())).toList();
        return CommentResponse.from(comments.create(publicationId, principal.getUserId(), request.text(), gallery));
    }

    @GetMapping("/comments")
    @PreAuthorize("hasAuthority('PUBLICATIONS_COMMENT_READ')")
    @RequiredPermission("PUBLICATIONS_COMMENT_READ")
    @Operation(summary = "List comments", description = "Newest first, with comment ID as the stable tie-breaker; response uses PageResponse.")
    @ApiResponse(responseCode = "200", description = "Page of comments")
    public PageResponse<CommentResponse> comments(@PathVariable Long publicationId, Pageable pageable) {
        return PageResponse.from(comments.list(publicationId, pageable).map(CommentResponse::from));
    }
}
