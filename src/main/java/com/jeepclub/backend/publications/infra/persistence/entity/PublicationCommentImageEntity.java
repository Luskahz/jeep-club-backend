package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class PublicationCommentImageEntity {
    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;
    @Column(name = "position", nullable = false)
    private int position;
}
