package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.platform.storage.image.ImageMediaService;
import com.jeepclub.backend.publications.core.domain.exception.PublicationNotFoundException;
import com.jeepclub.backend.publications.core.domain.model.*;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicationService {
    private final PublicationRepository publications;
    private final ImageMediaService images;
    private final Clock clock;

    @Transactional
    public Notice createNotice(Long authorUserId, String title, String content, List<PublicationImage> gallery) {
        Notice notice = Notice.create(authorUserId, title, content, gallery, Instant.now(clock));
        requireImages(notice.getImages());
        return (Notice) publications.save(notice);
    }

    @Transactional
    public Event createEvent(Long authorUserId, String title, String content, List<PublicationImage> gallery, Instant startsAt) {
        Event event = Event.create(authorUserId, title, content, gallery, startsAt, Instant.now(clock));
        requireImages(event.getImages());
        return (Event) publications.save(event);
    }

    @Transactional
    public Publication publish(Long id) {
        Publication publication = find(id);
        publication.publish(Instant.now(clock));
        return publications.save(publication);
    }

    @Transactional
    public Publication archive(Long id) {
        Publication publication = find(id);
        publication.archive(Instant.now(clock));
        return publications.save(publication);
    }

    @Transactional
    public Publication replaceImages(Long id, List<PublicationImage> gallery) {
        Publication publication = find(id);
        publication.replaceImages(gallery, Instant.now(clock));
        requireImages(publication.getImages());
        return publications.save(publication);
    }

    @Transactional
    public void delete(Long id, Long deletedByUserId) {
        publications.delete(id, deletedByUserId, Instant.now(clock));
    }

    private Publication find(Long id) {
        return publications.findByIdForUpdate(id).orElseThrow(() -> new PublicationNotFoundException(id));
    }

    private void requireImages(List<PublicationImage> gallery) {
        gallery.forEach(image -> images.requireExisting(image.storageKey()));
    }
}
