package com.jeepclub.backend.iam.identity.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "identity_user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfileEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_identity_user_profiles_user"))
    private UserEntity user;

    @Column(length = 150) private String occupation;
    @Column(length = 150) private String workplace;
    @Column(name = "postal_code", length = 8) private String postalCode;
    @Column(length = 150) private String street;
    @Column(name = "house_number", length = 20) private String number;
    @Column(length = 150) private String complement;
    @Column(length = 100) private String neighborhood;
    @Column(length = 100) private String city;
    @Column(length = 2) private String state;
}
