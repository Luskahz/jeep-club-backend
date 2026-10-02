package com.jeepclub.backend.publications.api.http.controller.member;

import com.jeepclub.backend.memberships.api.security.RequiresMembership;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.publications.api.http.dto.PublicationFeedDTO;
import com.jeepclub.backend.publications.core.application.service.PublicationFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.Clock;
import java.time.Instant;

@RestController
@RequestMapping("/publications")
@RequiredArgsConstructor
@RequiresMembership
@Tag(name = "Publication feed", description = "Chronological published Notice, Event and Service publications")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid filter", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Membership or permission required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Publication inaccessible", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class PublicationFeedController {
    private final PublicationFeedService feed;
    private final Clock clock;

    @GetMapping("/feed")
    @PreAuthorize("hasAuthority('PUBLICATIONS_FEED_READ')")
    @RequiredPermission("PUBLICATIONS_FEED_READ")
    @Operation(summary = "Read unified publication feed", description = "Only PUBLISHED concrete publications. Optional type=NOTICE|EVENT|SERVICE and inclusive publishedFrom/publishedTo filters. Ordered publishedAt DESC, id DESC. Social counts are batched.")
    @ApiResponse(responseCode = "200", description = "Page of visible publications")
    public PageResponse<PublicationFeedDTO.Item> feed(@RequestParam(required = false) String type,
                                                       @RequestParam(required = false) Instant publishedFrom,
                                                       @RequestParam(required = false) Instant publishedTo,
                                                       Pageable pageable, @AuthenticationPrincipal UserPrincipal principal) {
        var now = Instant.now(clock);
        return PageResponse.from(feed.feed(type, publishedFrom, publishedTo, pageable, principal.getUserId())
                .map(item -> PublicationFeedDTO.Item.from(item, now)));
    }

    @GetMapping("/{publicationId}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_PUBLICATION_READ')")
    @RequiredPermission("PUBLICATIONS_PUBLICATION_READ")
    @Operation(summary = "Read publication detail", description = "Only PUBLISHED concrete publications. Returns ordered full gallery and subtype details; Event FINISHED remains visible while editorially published.")
    @ApiResponse(responseCode = "200", description = "Visible publication detail")
    public PublicationFeedDTO.Detail detail(@PathVariable Long publicationId,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return PublicationFeedDTO.Detail.from(feed.detail(publicationId, principal.getUserId()), Instant.now(clock));
    }
}
