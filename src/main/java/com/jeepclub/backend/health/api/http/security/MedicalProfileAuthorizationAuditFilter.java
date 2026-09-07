package com.jeepclub.backend.health.api.http.security;

import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditEvent;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOperation;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOutcome;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.port.MedicalProfileAuditTrail;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MedicalProfileAuthorizationAuditFilter extends OncePerRequestFilter {

    private static final String ADMIN_PATH = "/admin/medical-profiles";
    private static final Pattern OWNER_PATH = Pattern.compile(
            "^/admin/medical-profiles/(users|dependents)/(\\d+)$"
    );

    private final Optional<MedicalProfileAuditTrail> auditTrail;
    private final Optional<Clock> clock;

    public MedicalProfileAuthorizationAuditFilter(
            Optional<MedicalProfileAuditTrail> auditTrail,
            Optional<Clock> clock
    ) {
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Long actorUserId = currentActorUserId();
        filterChain.doFilter(request, response);

        String path = request.getServletPath();
        if (!path.startsWith(ADMIN_PATH)
                || response.getStatus() != HttpStatus.FORBIDDEN.value()
                || auditTrail.isEmpty()) {
            return;
        }

        OwnerReference owner = ownerFrom(path);
        auditTrail.get().record(new MedicalProfileAuditEvent(
                actorUserId != null ? actorUserId : currentActorUserId(),
                owner.ownerType(),
                owner.ownerId(),
                operationFrom(request.getMethod()),
                MedicalProfileAuditOutcome.DENIED,
                Instant.now(clock.orElseGet(Clock::systemUTC))
        ));
    }

    private Long currentActorUserId() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }

    private OwnerReference ownerFrom(String path) {
        Matcher matcher = OWNER_PATH.matcher(path);
        if (!matcher.matches()) {
            return new OwnerReference(null, null);
        }

        MedicalProfileOwnerType ownerType = "users".equals(matcher.group(1))
                ? MedicalProfileOwnerType.USER
                : MedicalProfileOwnerType.DEPENDENT;
        return new OwnerReference(ownerType, Long.valueOf(matcher.group(2)));
    }

    private MedicalProfileAuditOperation operationFrom(String method) {
        return switch (method) {
            case "GET" -> MedicalProfileAuditOperation.READ;
            case "DELETE" -> MedicalProfileAuditOperation.DELETE;
            default -> MedicalProfileAuditOperation.UPDATE;
        };
    }

    private record OwnerReference(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
    }
}
