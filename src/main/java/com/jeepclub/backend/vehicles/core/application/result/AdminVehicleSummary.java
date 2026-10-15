package com.jeepclub.backend.vehicles.core.application.result;

import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;

public record AdminVehicleSummary(Vehicle vehicle, String ownerName) {}
