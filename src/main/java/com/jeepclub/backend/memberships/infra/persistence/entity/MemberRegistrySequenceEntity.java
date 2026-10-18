package com.jeepclub.backend.memberships.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity @Table(name="membership_registry_sequence")
@Getter @Setter @NoArgsConstructor
public class MemberRegistrySequenceEntity {
    @Id private Long id;
    @Column(name="next_value") private Long nextValue;
    @Column(name="format_signature", length=200) private String formatSignature;
    @Version @Column(nullable=false) private Long version;
}
