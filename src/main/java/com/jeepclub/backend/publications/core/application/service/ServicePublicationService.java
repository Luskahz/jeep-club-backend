package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.publications.core.application.exception.ServiceOperationException;
import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.exception.PublicationAlreadyDeletedException;
import com.jeepclub.backend.publications.core.domain.model.Publication;
import com.jeepclub.backend.publications.core.domain.model.ServicePublication;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import static com.jeepclub.backend.publications.core.application.exception.ServiceOperationException.Reason.*;

@Service
@RequiredArgsConstructor
public class ServicePublicationService {
    private final PublicationRepository publications;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ServicePublication findPublished(Long id) {
        ServicePublication service = asService(publications.findById(id).orElse(null));
        if (service.getStatus() != PublicationStatus.PUBLISHED) throw new ServiceOperationException(SERVICE_NOT_FOUND);
        return service;
    }

    @Transactional(readOnly = true)
    public ServicePublication findAdmin(Long id) {
        return asService(publications.findById(id).orElse(null));
    }

    @Transactional
    public void deleteOwn(Long id, Long userId) {
        ServicePublication service = locked(id);
        if (!service.getAuthorUserId().equals(userId)) throw new ServiceOperationException(SERVICE_NOT_OWNER);
        delete(id, userId);
    }

    @Transactional
    public void deleteAdmin(Long id, Long adminUserId) {
        locked(id);
        delete(id, adminUserId);
    }

    private void delete(Long id, Long actor) {
        try {
            publications.delete(id, actor, Instant.now(clock));
        } catch (PublicationAlreadyDeletedException exception) {
            throw new ServiceOperationException(SERVICE_NOT_FOUND);
        }
    }

    private ServicePublication locked(Long id) {
        return asService(publications.findByIdForUpdate(id).orElse(null));
    }

    private static ServicePublication asService(Publication publication) {
        if (publication instanceof ServicePublication service) return service;
        throw new ServiceOperationException(SERVICE_NOT_FOUND);
    }
}
