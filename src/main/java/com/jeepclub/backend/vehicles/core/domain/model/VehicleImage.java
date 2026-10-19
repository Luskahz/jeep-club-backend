package com.jeepclub.backend.vehicles.core.domain.model;

import com.jeepclub.backend.shared.storage.ImageReference;

/** Persisted references may be opaque legacy values. New associations use newReference. */
public record VehicleImage(String storageKey, int position, boolean primary) {
    public VehicleImage {
        if (storageKey == null || position < 0 || position > 4)
            throw new IllegalArgumentException("Invalid vehicle image reference or position.");
    }
    public static VehicleImage newReference(String key,int position,boolean primary) {
        return new VehicleImage(ImageReference.require(key),position,primary);
    }
    @Override public String toString() { return "VehicleImage[position="+position+", primary="+primary+"]"; }
}
