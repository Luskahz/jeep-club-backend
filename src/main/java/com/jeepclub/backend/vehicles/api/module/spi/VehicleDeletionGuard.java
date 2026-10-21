package com.jeepclub.backend.vehicles.api.module.spi;
/** Caller holds the aggregate lock; provider checks only its own retained references with current reads. */
public interface VehicleDeletionGuard { boolean hasRetainedReferences(Long id); }
