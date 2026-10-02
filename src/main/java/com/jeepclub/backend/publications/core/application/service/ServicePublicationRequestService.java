package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.core.domain.model.PublicationImage;
import com.jeepclub.backend.publications.core.domain.model.ServicePublicationRequest;
import com.jeepclub.backend.publications.core.repository.ServicePublicationRequestRepository;
import com.jeepclub.backend.publications.core.application.exception.ServiceOperationException;
import static com.jeepclub.backend.publications.core.application.exception.ServiceOperationException.Reason.REQUEST_NOT_FOUND;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicePublicationRequestService {
    private final ServicePublicationRequestRepository requests;
    private final ImageMediaService images;
    private final Clock clock;

    @Transactional
    public ServicePublicationRequest create(Long requestedByUserId, String title, String content,
                                            BigDecimal amount, String contactPhone, List<PublicationImage> gallery) {
        var request = ServicePublicationRequest.create(requestedByUserId, title, content,
                amount, contactPhone, gallery, Instant.now(clock));
        request.getImages().forEach(image -> images.requireExisting(image.storageKey()));
        return requests.save(request);
    }

    @Transactional(readOnly = true)
    public ServicePublicationRequest findOwn(Long id, Long userId) {
        var request = requests.findById(id).orElseThrow(() -> new ServiceOperationException(REQUEST_NOT_FOUND));
        if (!request.getRequestedByUserId().equals(userId)) throw new ServiceOperationException(REQUEST_NOT_FOUND);
        return request;
    }
}
