package com.jeepclub.backend.publications.api.http.controller.admin;
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

import com.jeepclub.backend.publications.core.application.service.AdminEventService;
import com.jeepclub.backend.billing.api.module.EventChargeCatalogQuery;
import com.jeepclub.backend.health.api.module.medicalprofile.*;
import tools.jackson.databind.JsonNode;
@RestController @RequestMapping("/admin/events") @RequiredArgsConstructor
@Tag(name="Events administration", description="Event lifecycle, finance configuration, guests, rides and individual emergency health access.")
@ApiResponses({
    @ApiResponse(responseCode="400", description="Event validation, authorization or state error; RFC 9457 code identifies the reason.", content=@Content(mediaType="application/problem+json", schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="401", description="Event validation, authorization or state error; RFC 9457 code identifies the reason.", content=@Content(mediaType="application/problem+json", schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="403", description="Event validation, authorization or state error; RFC 9457 code identifies the reason.", content=@Content(mediaType="application/problem+json", schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="404", description="Event validation, authorization or state error; RFC 9457 code identifies the reason.", content=@Content(mediaType="application/problem+json", schema=@Schema(implementation=ApiErrorResponse.class))),
    @ApiResponse(responseCode="409", description="Event validation, authorization or state error; RFC 9457 code identifies the reason.", content=@Content(mediaType="application/problem+json", schema=@Schema(implementation=ApiErrorResponse.class)))
})

public class AdminEventController {
    private final AdminEventService service;
    private final EventRequestReader reader;
    private final Clock clock;
    @GetMapping
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_READ_ADMIN')")
    @RequiredPermission("PUBLICATIONS_EVENT_READ_ADMIN")
    @Operation(summary="Page through administrative events in every editorial state", responses=@ApiResponse(responseCode="200",description="Event page"))
    public PageResponse<EventResponseDTO> list(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        return PageResponse.from(service.findAll(pageable).map(this::response));
    }
    @PostMapping("")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_CREATE')")
    @RequiredPermission("PUBLICATIONS_EVENT_CREATE")
    @Operation(summary="Create DRAFT publication and OPEN event", description="Future startsAt is mandatory; endsAt must be later. Inline charges are ACTIVE ONE_TIME with AFTER_DUE_DATE. Each charge may set a financialDueDate (LocalDate) independently of participationCutoff; null uses the Event start date as a technical dueDate with AFTER_DUE_DATE regularization. Required participation charges also require AFTER_DUE_DATE.",
        requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(required=true,content=@Content(schema=@Schema(implementation=CreateEventRequestDTO.class))))
    @ApiResponse(responseCode="201", description="Create DRAFT publication and OPEN event")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDTO create(@RequestBody JsonNode body, @AuthenticationPrincipal UserPrincipal principal) {
        var r = reader.read(body, true);
        return response(service.create(principal.getUserId(), r.title(), r.content(), images(r.images()), r.startsAt(), r.endsAt(), charges(r.charges())));
    }
    @GetMapping("/charge-catalog")
    @PreAuthorize("hasAnyAuthority('PUBLICATIONS_EVENT_CREATE', 'PUBLICATIONS_EVENT_UPDATE')")
    @RequiredPermission({"PUBLICATIONS_EVENT_CREATE", "PUBLICATIONS_EVENT_UPDATE"})
    @Operation(summary="List ACTIVE ONE_TIME Billing definitions", description="Requires either EVENT_CREATE or EVENT_UPDATE, allowing the catalog during creation and editing.")
    @ApiResponse(responseCode="200", description="List ACTIVE ONE_TIME Billing definitions")
    public PageResponse<EventChargeCatalogQuery.Entry> catalog(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        return PageResponse.from(service.catalog(pageable));
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_READ_ADMIN')")
    @RequiredPermission("PUBLICATIONS_EVENT_READ_ADMIN")
    @Operation(summary="Read event in any editorial state")
    @ApiResponse(responseCode="200", description="Read event in any editorial state")
    public EventResponseDTO read(@PathVariable Long id) {
        return response(service.findById(id));
    }
    @GetMapping("/{id}/charges")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_READ_ADMIN')")
    @RequiredPermission("PUBLICATIONS_EVENT_READ_ADMIN")
    @Operation(summary="Read event participation charge rules")
    @ApiResponse(responseCode="200", description="Read event participation charge rules")
    public List<EventChargeRule> rules(@PathVariable Long id) {
        return service.rules(id);
    }
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_UPDATE')")
    @RequiredPermission("PUBLICATIONS_EVENT_UPDATE")
    @Operation(summary="Partial update; finance and schedule locked after registration", description="Omitted top-level fields are preserved; explicit null is allowed for endsAt. When charges is provided it replaces the complete charge configuration: absent or null financialDueDate means no Event-defined final date. Charge configuration, including financialDueDate, is frozen after any registration. No financial snapshots are rewritten.",
        requestBody=@io.swagger.v3.oas.annotations.parameters.RequestBody(required=true,content=@Content(schema=@Schema(implementation=EventRequestDTO.class))))
    @ApiResponse(responseCode="200", description="Partial update; finance and schedule locked after registration")
    public EventResponseDTO update(@PathVariable Long id, @RequestBody JsonNode body) {
        var r = reader.read(body, false);
        return response(service.update(id, r.title(), r.content(), images(r.images()), r.startsAt(), body.has("endsAt"), r.endsAt(), charges(r.charges())));
    }
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_PUBLISH')")
    @RequiredPermission("PUBLICATIONS_EVENT_PUBLISH")
    @Operation(summary="publish event without changing unrelated lifecycle")
    @ApiResponse(responseCode="200", description="publish event without changing unrelated lifecycle")
    public EventResponseDTO publish(@PathVariable Long id) {
        return response(service.publish(id));
    }
    @PostMapping("/{id}/finish")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_FINISH')")
    @RequiredPermission("PUBLICATIONS_EVENT_FINISH")
    @Operation(summary="finish event without changing unrelated lifecycle")
    @ApiResponse(responseCode="200", description="finish event without changing unrelated lifecycle")
    public EventResponseDTO finish(@PathVariable Long id) {
        return response(service.finish(id));
    }
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_CANCEL')")
    @RequiredPermission("PUBLICATIONS_EVENT_CANCEL")
    @Operation(summary="Cancel event and its own financial cycles")
    @ApiResponse(responseCode="200", description="Cancel event and its own financial cycles")
    public EventResponseDTO cancel(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return response(service.cancel(id, principal.getUserId()));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_DELETE')")
    @RequiredPermission("PUBLICATIONS_EVENT_DELETE")
    @Operation(summary="Hard delete with history; retain financial and participation facts")
    @ApiResponse(responseCode="204", description="Hard delete with history; retain financial and participation facts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(id, principal.getUserId());
    }
    @GetMapping("/{id}/dashboard")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_ADMIN_DASHBOARD_READ')")
    @RequiredPermission("PUBLICATIONS_EVENT_ADMIN_DASHBOARD_READ")
    @Operation(summary="Read operational totals and private financial status")
    @ApiResponse(responseCode="200", description="Read operational totals and private financial status")
    public AdminEventService.Dashboard dashboard(@PathVariable Long id) {
        return service.dashboard(id);
    }
    @GetMapping("/{id}/guest-requests")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_GUEST_ADMIN_READ')")
    @RequiredPermission("PUBLICATIONS_EVENT_GUEST_ADMIN_READ")
    @Operation(summary="Read guests and historical approved visit count")
    @ApiResponse(responseCode="200", description="Read guests and historical approved visit count")
    public PageResponse<GuestReview> guests(@PathVariable Long id, @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        var guests = service.guests(id).stream().sorted(java.util.Comparator.comparing(EventGuestRequest::id)).toList();
        var counts = service.visits(guests.stream().map(EventGuestRequest::cpf).toList());
        return EventPages.of(guests.stream().map(g -> new GuestReview(g, counts.getOrDefault(g.cpf(),0L))).toList(), pageable);
    }
    @PostMapping("/{id}/guest-requests")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_GUEST_ADMIN_CREATE')")
    @RequiredPermission("PUBLICATIONS_EVENT_GUEST_ADMIN_CREATE")
    @Operation(summary="Create administrative guest without transport")
    @ApiResponse(responseCode="201", description="Create administrative guest without transport")
    @ResponseStatus(HttpStatus.CREATED)
    public EventGuestRequest guest(@PathVariable Long id, @Valid @RequestBody EventOperationRequests.Guest r, @AuthenticationPrincipal UserPrincipal principal) {
        if (r.vehicleId() != null) throw new IllegalArgumentException("Administrative guest uses ride selection."); return service.createGuest(id, principal.getUserId(), r.cpf(), r.guestName());
    }
    @PostMapping("/{id}/guest-requests/{guestId}/approve")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_GUEST_APPROVE')")
    @RequiredPermission("PUBLICATIONS_EVENT_GUEST_APPROVE")
    @Operation(summary="Approve guest with atomic capacity check")
    @ApiResponse(responseCode="200", description="Approve guest with atomic capacity check")
    public EventGuestRequest approve(@PathVariable Long id, @PathVariable Long guestId, @AuthenticationPrincipal UserPrincipal principal) {
        return service.reviewGuest(id, guestId, principal.getUserId(), true, null);
    }
    @PostMapping("/{id}/guest-requests/{guestId}/reject")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_GUEST_REJECT')")
    @RequiredPermission("PUBLICATIONS_EVENT_GUEST_REJECT")
    @Operation(summary="Reject guest without consuming a seat")
    @ApiResponse(responseCode="200", description="Reject guest without consuming a seat")
    public EventGuestRequest reject(@PathVariable Long id, @PathVariable Long guestId, @Valid @RequestBody EventOperationRequests.Rejection r, @AuthenticationPrincipal UserPrincipal principal) {
        return service.reviewGuest(id, guestId, principal.getUserId(), false, r.reason());
    }
    @GetMapping("/{id}/ride-offers")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_READ_ADMIN')")
    @RequiredPermission("PUBLICATIONS_EVENT_READ_ADMIN")
    @Operation(summary="Read accepted and declined ride offers")
    @ApiResponse(responseCode="200", description="Read accepted and declined ride offers")
    public PageResponse<EventRideOffer> offers(@PathVariable Long id, @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        return EventPages.of(service.offers(id).stream().sorted(java.util.Comparator.comparing(EventRideOffer::id)).toList(),pageable);
    }
    @PostMapping("/{id}/ride-offers/{offerId}/select")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_RIDE_OFFER_SELECT')")
    @RequiredPermission("PUBLICATIONS_EVENT_RIDE_OFFER_SELECT")
    @Operation(summary="Select accepted ride; fail if capacity changed")
    @ApiResponse(responseCode="200", description="Select accepted ride; fail if capacity changed")
    public EventRideOffer select(@PathVariable Long id, @PathVariable Long offerId, @AuthenticationPrincipal UserPrincipal principal) {
        return service.select(id, offerId, principal.getUserId());
    }
    @GetMapping("/{id}/health/{type}/{participantId}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ')")
    @RequiredPermission("PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ")
    @Operation(summary="Leitura médica individual auditada de participante confirmado, independente do lifecycle temporal")
    @ApiResponse(responseCode="200", description="Leitura médica individual auditada de participante confirmado, independente do lifecycle temporal")
    public EmergencyMedicalProfileQuery.Profile health(@PathVariable Long id, @PathVariable MedicalProfileOwner type, @PathVariable Long participantId, @AuthenticationPrincipal UserPrincipal principal) {
        return service.health(id, type, participantId, principal.getUserId());
    }
    public record GuestReview(EventGuestRequest request, long approvedHistoricalVisits) {}
    private EventResponseDTO response(Event e) { return EventResponseDTO.from(e, Instant.now(clock)); }
    private List<PublicationImage> images(List<PublicationImageRequestDTO> list) {
        return list == null ? null : list.stream().map(i -> new PublicationImage(i.storageKey(), i.position(), i.primary())).toList();
    }
    private List<AdminEventService.ChargeConfiguration> charges(List<EventRequestDTO.Charge> list) {
        return list == null ? null : list.stream().map(c -> new AdminEventService.ChargeConfiguration(c.chargeDefinitionId(), c.name(), c.description(), c.amount(), Boolean.TRUE.equals(c.billingRequired()), Boolean.TRUE.equals(c.requiredForParticipation()), c.participationCutoff(), c.financialDueDate())).toList();
    }
}
