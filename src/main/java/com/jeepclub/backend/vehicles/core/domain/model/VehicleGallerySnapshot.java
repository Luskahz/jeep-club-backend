package com.jeepclub.backend.vehicles.core.domain.model;
import java.time.Instant;
public record VehicleGallerySnapshot(Long historyId,Long vehicleId,Long ownerId,Long deletedByUserId,
        Instant deletedAt,long galleryRevision,VehicleGallery gallery) {
    @Override public String toString() { return "VehicleGallerySnapshot[historyId="+historyId+", size="+gallery.images().size()+"]"; }
}
