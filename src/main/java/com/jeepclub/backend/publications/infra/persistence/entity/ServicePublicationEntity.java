package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "publication_services", uniqueConstraints =
        @UniqueConstraint(name = "uk_publication_service_source_request", columnNames = "source_request_id"))
@PrimaryKeyJoinColumn(name = "publication_id")
@Getter
@Setter
public class ServicePublicationEntity extends PublicationEntity {
    @Column(name = "source_request_id", nullable = false, updatable = false)
    private Long sourceRequestId;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(name = "contact_phone", nullable = false, length = 30)
    private String contactPhone;
}
