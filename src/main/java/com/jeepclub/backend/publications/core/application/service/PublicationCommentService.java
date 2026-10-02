package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.core.domain.exception.PublicationNotFoundException;
import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.model.PublicationComment;
import com.jeepclub.backend.publications.core.domain.model.PublicationCommentImage;
import com.jeepclub.backend.publications.core.repository.PublicationCommentRepository;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
public class PublicationCommentService {
    private final PublicationRepository publications;
    private final PublicationCommentRepository comments;
    private final ImageMediaService images;
    private final Clock clock;

    @Transactional
    public PublicationComment create(Long publicationId, Long authorUserId, String content,
                                     List<PublicationCommentImage> gallery) {
        PublicationComment comment = PublicationComment.create(publicationId, authorUserId, content, gallery, Instant.now(clock));
        requirePublished(publicationId);
        comment.getImages().forEach(image -> images.requireExisting(image.storageKey()));
        return comments.save(comment);
    }

    @Transactional(readOnly = true)
    public Page<PublicationComment> list(Long publicationId, Pageable pageable) {
        requirePublishedForRead(publicationId);
        var stable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return comments.findByPublication(publicationId, stable);
    }

    private void requirePublished(Long publicationId) {
        var publication = publications.findByIdForUpdate(publicationId)
                .orElseThrow(() -> new PublicationNotFoundException(publicationId));
        if (publication.getStatus() != PublicationStatus.PUBLISHED) throw new PublicationNotFoundException(publicationId);
    }

    private void requirePublishedForRead(Long publicationId) {
        var publication = publications.findById(publicationId)
                .orElseThrow(() -> new PublicationNotFoundException(publicationId));
        if (publication.getStatus() != PublicationStatus.PUBLISHED) throw new PublicationNotFoundException(publicationId);
    }
}
