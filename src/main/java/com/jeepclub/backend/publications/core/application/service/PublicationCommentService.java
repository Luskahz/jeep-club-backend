package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.core.domain.exception.PublicationNotFoundException;
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
        publications.findById(publicationId).orElseThrow(() -> new PublicationNotFoundException(publicationId));
        comment.getImages().forEach(image -> images.requireExisting(image.storageKey()));
        return comments.save(comment);
    }
}
