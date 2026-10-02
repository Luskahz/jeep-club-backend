package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "publication_service_history")
@PrimaryKeyJoinColumn(name = "history_id")
@Getter
@Setter
public class ServicePublicationHistoryEntity extends PublicationHistoryEntity {
    @Column(name = "source_request_id", nullable = false, updatable = false)
    private Long sourceRequestId;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(name = "contact_phone", nullable = false, length = 30)
    private String contactPhone;
}
