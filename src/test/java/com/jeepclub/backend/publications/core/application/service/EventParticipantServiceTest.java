package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.billing.api.module.EventFinancialQuery;
import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.publications.core.application.service.internal.EventOperations;
import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.model.Event;
import com.jeepclub.backend.publications.core.repository.EventOperationsRepository;
import com.jeepclub.backend.publications.core.repository.EventParticipantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.Collection;
import java.util.stream.LongStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventParticipantServiceTest {
    @Test void resolvesLargePagesInIdentityBatchesWithoutLosingOrder() {
        var events = mock(EventOperations.class);
        var event = mock(Event.class);
        var registrations = mock(EventParticipantRepository.class);
        var users = mock(UserQuery.class);
        var pageable = PageRequest.of(0, 501);
        var ids = LongStream.rangeClosed(1, 501).boxed().toList();
        var publications = mock(com.jeepclub.backend.publications.core.repository.PublicationRepository.class);
        when(publications.findById(1L)).thenReturn(java.util.Optional.of(event));
        when(event.getStatus()).thenReturn(PublicationStatus.PUBLISHED);
        when(registrations.confirmedUserIds(1L, pageable)).thenReturn(new PageImpl<>(ids, pageable, 501));
        when(users.findByIds(anyCollection())).thenAnswer(call -> ((Collection<Long>) call.getArgument(0)).stream()
            .map(id -> new UserDetails(id, "Member " + id, null, null, null, null, null, null, true, null, null, null)).toList());
        var service = new EventService(events, publications, mock(EventOperationsRepository.class), mock(EventFinancialQuery.class), registrations, users);
        var result = service.participants(1L, pageable);
        assertThat(result.getContent()).hasSize(501);
        assertThat(result.getContent().get(500).userId()).isEqualTo(501);
        verify(users).findByIds(ids.subList(0, 500));
        verify(users).findByIds(ids.subList(500, 501));
        verifyNoMoreInteractions(users);
        verifyNoInteractions(events);
    }
}
