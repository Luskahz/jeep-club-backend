package com.jeepclub.backend.publications.api.http.controller.member;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.publications.api.http.dto.*;
import com.jeepclub.backend.publications.core.domain.model.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;

import com.jeepclub.backend.publications.core.application.service.EventService;
import com.jeepclub.backend.memberships.api.security.RequiresMembership;
@RestController @RequestMapping("/events") @RequiredArgsConstructor @RequiresMembership
@Tag(name="Events", description="Member registration and own guest/ride operations. Identity comes from the authenticated principal.")
@ApiResponses({
    @ApiResponse(responseCode="400", description="Event validation, authorization or state error; RFC 9457 code identifies the reason.", content=@Content(mediaType="application/problem+json", schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="401", description="Event validation, authorization or state error; RFC 9457 code identifies the reason.", content=@Content(mediaType="application/problem+json", schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="403", description="Event validation, authorization or state error; RFC 9457 code identifies the reason.", content=@Content(mediaType="application/problem+json", schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="404", description="Event validation, authorization or state error; RFC 9457 code identifies the reason.", content=@Content(mediaType="application/problem+json", schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="409", description="Event validation, authorization or state error; RFC 9457 code identifies the reason.", content=@Content(mediaType="application/problem+json", schema=@Schema(implementation=ApiErrorResponse.class)))
})

public class EventController {
    private final EventService service;
    private final Clock clock;
    @PutMapping("/{id}/registrations/me/allocations")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_REGISTER')")
    @RequiredPermission("PUBLICATIONS_EVENT_REGISTER")
    @Operation(summary="Replace own vehicle allocation before start; approved guests retain their seat", responses=@ApiResponse(responseCode="200",description="Allocation updated atomically"))
    public EventRegistration allocate(@PathVariable Long id, @Valid @RequestBody EventOperationRequests.Registration r, @AuthenticationPrincipal UserPrincipal principal) {
        return service.allocate(id, principal.getUserId(), r.allocations().stream().map(a -> new EventRegistration.Allocation(a.vehicleId(), a.member(), a.dependentIds())).toList(), r.dependentIds());
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_READ')")
    @RequiredPermission("PUBLICATIONS_EVENT_READ")
    @Operation(summary="Read published event without private financial data")
    @ApiResponse(responseCode="200", description="Read published event without private financial data")
    public EventResponseDTO read(@PathVariable Long id) {
        return EventResponseDTO.from(service.findById(id), Instant.now(clock));
    }
    @PostMapping("/{id}/registrations")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_REGISTER')")
    @RequiredPermission("PUBLICATIONS_EVENT_REGISTER")
    @Operation(summary="Register and ensure charges; confirm only when all participation requirements satisfied",description="Creates one registration per event and principal. Required charges yield PENDING_PAYMENT until every charge is PAID or has PENDING_VALIDATION submitted by cutoff. Without required charges, confirms immediately. CONFIRMED never regresses after financial rejection. CANCELLED preserves history.")
    @ApiResponse(responseCode="201", description="Register and ensure charges; confirm only when all participation requirements satisfied")
    @ResponseStatus(HttpStatus.CREATED)
    public EventRegistration register(@PathVariable Long id, @Valid @RequestBody EventOperationRequests.Registration r, @AuthenticationPrincipal UserPrincipal principal) {
        return service.register(id, principal.getUserId(), r.allocations().stream().map(a -> new EventRegistration.Allocation(a.vehicleId(), a.member(), a.dependentIds())).toList(), r.dependentIds());
    }
    @GetMapping("/{id}/registrations/me")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_REGISTRATION_READ')")
    @RequiredPermission("PUBLICATIONS_EVENT_REGISTRATION_READ")
    @Operation(summary="Read own registration; refresh payment requirements without downgrading confirmation")
    @ApiResponse(responseCode="200", description="Read own registration; refresh payment requirements without downgrading confirmation")
    public EventRegistration mine(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return service.mine(id, principal.getUserId());
    }
    @PostMapping("/{id}/registrations/me/cancel")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_REGISTRATION_CANCEL')")
    @RequiredPermission("PUBLICATIONS_EVENT_REGISTRATION_CANCEL")
    @Operation(summary="Cancel own registration before start; preserve history")
    @ApiResponse(responseCode="200", description="Cancel own registration before start; preserve history")
    public EventRegistration cancel(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return service.cancel(id, principal.getUserId());
    }
    @PostMapping("/{id}/guest-requests")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_GUEST_REQUEST_CREATE')")
    @RequiredPermission("PUBLICATIONS_EVENT_GUEST_REQUEST_CREATE")
    @Operation(summary="Request guest for own allocated vehicle")
    @ApiResponse(responseCode="201", description="Request guest for own allocated vehicle")
    @ResponseStatus(HttpStatus.CREATED)
    public EventGuestRequest guest(@PathVariable Long id, @Valid @RequestBody EventOperationRequests.Guest r, @AuthenticationPrincipal UserPrincipal principal) {
        return service.requestGuest(id, principal.getUserId(), r.vehicleId(), r.cpf());
    }
    @GetMapping("/{id}/guest-requests")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_GUEST_REQUEST_READ')")
    @RequiredPermission("PUBLICATIONS_EVENT_GUEST_REQUEST_READ")
    @Operation(summary="Read own guest requests")
    @ApiResponse(responseCode="200", description="Read own guest requests")
    public PageResponse<EventGuestRequest> guests(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        return EventPages.of(service.guests(id, principal.getUserId()).stream().sorted(java.util.Comparator.comparing(EventGuestRequest::id)).toList(),pageable);
    }
    @GetMapping("/{id}/ride-requests")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_RIDE_OFFER_READ')")
    @RequiredPermission("PUBLICATIONS_EVENT_RIDE_OFFER_READ")
    @Operation(summary="List administrative guest request IDs eligible for own spare seats")
    @ApiResponse(responseCode="200", description="List administrative guest request IDs eligible for own spare seats")
    public PageResponse<Long> rides(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        return EventPages.of(service.rideRequests(id, principal.getUserId()).stream().sorted().toList(),pageable);
    }
    @PostMapping("/{id}/ride-requests/{guestId}/response")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_RIDE_OFFER_RESPOND')")
    @RequiredPermission("PUBLICATIONS_EVENT_RIDE_OFFER_RESPOND")
    @Operation(summary="Accept or decline using own vehicle; acceptance does not reserve capacity")
    @ApiResponse(responseCode="201", description="Accept or decline using own vehicle; acceptance does not reserve capacity")
    @ResponseStatus(HttpStatus.CREATED)
    public EventRideOffer respond(@PathVariable Long id, @PathVariable Long guestId, @Valid @RequestBody EventOperationRequests.Ride r, @AuthenticationPrincipal UserPrincipal principal) {
        return service.respond(id, guestId, principal.getUserId(), r.vehicleId(), r.accept());
    }
}
