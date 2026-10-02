package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.core.application.exception.ServiceOperationException;
import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.model.PublicationImage;
import com.jeepclub.backend.publications.core.domain.model.ServicePublication;
import com.jeepclub.backend.publications.core.domain.model.ServicePublicationChangeRequest;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import com.jeepclub.backend.publications.core.repository.ServicePublicationChangeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import static com.jeepclub.backend.publications.core.application.exception.ServiceOperationException.Reason.*;

@Service
@RequiredArgsConstructor
public class ServicePublicationChangeRequestService {
    private final PublicationRepository publications;
    private final ServicePublicationChangeRequestRepository changes;
    private final ImageMediaService media;
    private final Clock clock;

    @Transactional
    public ServicePublicationChangeRequest create(Long serviceId, Long userId, String title, String content,
            BigDecimal amount, String contactPhone, List<PublicationImage> images) {
        if (title == null && content == null && amount == null && contactPhone == null && images == null)
            throw new ServiceOperationException(INVALID_REQUEST);
        var publication = publications.findByIdForUpdate(serviceId).orElse(null);
        if (!(publication instanceof ServicePublication service)) throw new ServiceOperationException(SERVICE_NOT_FOUND);
        if (!service.getAuthorUserId().equals(userId)) throw new ServiceOperationException(SERVICE_NOT_OWNER);
        if (service.getStatus() != PublicationStatus.PUBLISHED) throw new ServiceOperationException(SERVICE_INVALID_STATE);
        if (changes.existsPendingForService(serviceId)) throw new ServiceOperationException(CHANGE_REQUEST_ALREADY_PENDING);
        Instant now = Instant.now(clock);
        var change = ServicePublicationChangeRequest.create(serviceId, userId,
                title == null ? service.getTitle() : title,
                content == null ? service.getContent() : content,
                amount == null ? service.getAmount() : amount,
                contactPhone == null ? service.getContactPhone() : contactPhone,
                images == null ? service.getImages() : images, now);
        if (images != null) change.getProposedImages().forEach(image -> media.requireExisting(image.storageKey()));
        return changes.save(change);
    }

    @Transactional(readOnly = true)
    public ServicePublicationChangeRequest findOwn(Long id, Long userId) {
        var change = changes.findById(id).orElseThrow(() -> new ServiceOperationException(CHANGE_REQUEST_NOT_FOUND));
        if (!change.getRequestedByUserId().equals(userId)) throw new ServiceOperationException(CHANGE_REQUEST_NOT_FOUND);
        return change;
    }
}
