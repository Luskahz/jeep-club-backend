package com.jeepclub.backend.publications.core.repository;

import com.jeepclub.backend.publications.core.domain.model.PublicationLike;

public interface PublicationLikeRepository {
    PublicationLike save(PublicationLike like);
}
