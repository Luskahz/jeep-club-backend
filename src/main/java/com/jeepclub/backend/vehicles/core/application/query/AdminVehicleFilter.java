package com.jeepclub.backend.vehicles.core.application.query;

import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;

public record AdminVehicleFilter(String q, String plate, Long ownerId, String nickname, String model,
        VehicleStatus status) {
    public AdminVehicleFilter {
        q=trim(q); plate=normalizePlate(plate); nickname=trim(nickname); model=trim(model);
        if(plate!=null && plate.isEmpty()) throw new com.jeepclub.backend.vehicles.core.application.exceptions.InvalidVehicleSearchException("Plate filter must contain a searchable character.");
        status=status==null?VehicleStatus.ACTIVE:status;
    }
    public static String normalizePlate(String value) {String text=trim(value);return text==null?null:text.replaceAll("[\\s.\\-]", "");}
    private static String trim(String value) {return value==null||value.isBlank()?null:value.trim();}
}
