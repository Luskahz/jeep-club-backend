package com.jeepclub.backend.vehicles.core.application.query;

import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import java.util.Set;

public record AdminVehicleFilter(String q, String plate, Long ownerId, String nickname, String model,
        VehicleStatus status, Set<Long> queryOwnerIds) {
    public AdminVehicleFilter {
        q=trim(q); plate=normalizePlate(plate); nickname=trim(nickname); model=trim(model);
        if(plate!=null && plate.isEmpty()) throw new com.jeepclub.backend.vehicles.core.application.exceptions.InvalidVehicleSearchException("Plate filter must contain a searchable character.");
        status=status==null?VehicleStatus.ACTIVE:status;
        queryOwnerIds=queryOwnerIds==null?Set.of():Set.copyOf(queryOwnerIds);
        if(queryOwnerIds.size()>500) throw new com.jeepclub.backend.vehicles.core.application.exceptions.InvalidVehicleSearchException("Owner match limit exceeded.");
    }
    public AdminVehicleFilter withOwners(Set<Long> owners) {return new AdminVehicleFilter(q,plate,ownerId,nickname,model,status,owners);}
    public static String normalizePlate(String value) {String text=trim(value);return text==null?null:text.replaceAll("[\\s.\\-]", "");}
    private static String trim(String value) {return value==null||value.isBlank()?null:value.trim();}
}
