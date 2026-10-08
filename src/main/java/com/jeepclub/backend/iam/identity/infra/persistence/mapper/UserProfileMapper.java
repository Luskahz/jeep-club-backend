package com.jeepclub.backend.iam.identity.infra.persistence.mapper;

import com.jeepclub.backend.iam.identity.core.domain.model.*;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserProfileEntity;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {
    public UserProfile toDomain(UserProfileEntity entity) {
        WorkProfile work = entity.getOccupation() == null && entity.getWorkplace() == null ? null
                : new WorkProfile(entity.getOccupation(), entity.getWorkplace());
        ResidentialAddress address = entity.getPostalCode() == null && entity.getStreet() == null
                && entity.getNumber() == null && entity.getComplement() == null && entity.getNeighborhood() == null
                && entity.getCity() == null && entity.getState() == null ? null
                : new ResidentialAddress(entity.getPostalCode(), entity.getStreet(), entity.getNumber(),
                entity.getComplement(), entity.getNeighborhood(), entity.getCity(), entity.getState());
        return new UserProfile(entity.getUserId(), work, address);
    }

    public void copy(UserProfile profile, UserProfileEntity entity) {
        WorkProfile work = profile.workProfile();
        ResidentialAddress address = profile.address();
        entity.setOccupation(work == null ? null : work.occupation());
        entity.setWorkplace(work == null ? null : work.workplace());
        entity.setPostalCode(address == null ? null : address.postalCode());
        entity.setStreet(address == null ? null : address.street());
        entity.setNumber(address == null ? null : address.number());
        entity.setComplement(address == null ? null : address.complement());
        entity.setNeighborhood(address == null ? null : address.neighborhood());
        entity.setCity(address == null ? null : address.city());
        entity.setState(address == null ? null : address.state());
    }
}
