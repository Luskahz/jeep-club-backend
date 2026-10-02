package com.jeepclub.backend.publications.core.application.service;

import com.jeepclub.backend.publications.core.repository.PublicationCommentRepository;
import com.jeepclub.backend.publications.core.repository.PublicationLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicationSocialSummaryQuery {
    private final PublicationLikeRepository likes;
    private final PublicationCommentRepository comments;

    public record Summary(long likeCount, long commentCount, boolean likedByMe) {}

    @Transactional(readOnly = true)
    public Map<Long, Summary> get(Collection<Long> publicationIds, Long memberUserId) {
        if (publicationIds.isEmpty()) return Map.of();
        var likeCounts = likes.counts(publicationIds);
        var commentCounts = comments.counts(publicationIds);
        var liked = likes.likedByMember(publicationIds, memberUserId);
        return publicationIds.stream().distinct().collect(Collectors.toUnmodifiableMap(id -> id,
                id -> new Summary(likeCounts.getOrDefault(id, 0L), commentCounts.getOrDefault(id, 0L), liked.contains(id))));
    }
}
