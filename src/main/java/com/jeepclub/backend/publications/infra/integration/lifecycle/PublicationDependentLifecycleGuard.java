package com.jeepclub.backend.publications.infra.integration.lifecycle;
import com.jeepclub.backend.dependents.api.module.spi.DependentLifecycleGuard;
import com.jeepclub.backend.publications.infra.persistence.adapter.EventLifecycleParticipation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component @RequiredArgsConstructor
class PublicationDependentLifecycleGuard implements DependentLifecycleGuard {
    private final EventLifecycleParticipation participation;
    public boolean hasOperationalParticipation(Long id,Instant now) {return participation.dependent(id,now);}
}
