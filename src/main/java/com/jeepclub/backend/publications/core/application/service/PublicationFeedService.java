package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.publications.core.domain.enums.PublicationStatus;
import com.jeepclub.backend.publications.core.domain.exception.PublicationNotFoundException;
import com.jeepclub.backend.publications.core.domain.model.Publication;
import com.jeepclub.backend.publications.core.repository.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PublicationFeedService {
    private final PublicationRepository publications;
    private final PublicationSocialSummaryQuery social;

    public record Item(Publication publication, PublicationSocialSummaryQuery.Summary summary) {}

    @Transactional(readOnly = true)
    public Page<Item> feed(String type, Instant from, Instant to, Pageable pageable, Long memberUserId) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Published period is reversed.");
        }
        if (type != null && !java.util.Set.of("NOTICE", "EVENT", "SERVICE").contains(type)) {
            throw new IllegalArgumentException("Unsupported publication type.");
        }
        var stable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));
        var page = publications.findPublished(type, from, to, stable);
        Map<Long, PublicationSocialSummaryQuery.Summary> summaries = social.get(
                page.getContent().stream().map(Publication::getId).toList(), memberUserId);
        return page.map(publication -> new Item(publication, summaries.get(publication.getId())));
    }

    @Transactional(readOnly = true)
    public Item detail(Long id, Long memberUserId) {
        var publication = publications.findById(id).orElseThrow(() -> new PublicationNotFoundException(id));
        if (publication.getStatus() != PublicationStatus.PUBLISHED) throw new PublicationNotFoundException(id);
        return new Item(publication, social.get(java.util.List.of(id), memberUserId).get(id));
    }
}
