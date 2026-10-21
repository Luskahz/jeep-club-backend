package com.jeepclub.backend.vehicles.api.http.dto.gallery;
import com.jeepclub.backend.vehicles.core.domain.model.VehicleGallerySnapshot;
import java.time.Instant;
import java.util.List;
public record VehicleGalleryHistoryResponseDTO(Long historyId,Long vehicleId,Long ownerId,Long deletedByUserId,
        Instant deletedAt,long galleryRevision,List<VehicleImageResponseDTO> images) {
    public static VehicleGalleryHistoryResponseDTO from(VehicleGallerySnapshot h) {
        return new VehicleGalleryHistoryResponseDTO(h.historyId(),h.vehicleId(),h.ownerId(),h.deletedByUserId(),h.deletedAt(),h.galleryRevision(),h.gallery().images().stream().map(VehicleImageResponseDTO::from).toList());
    }
    @Override public String toString() { return "VehicleGalleryHistoryResponseDTO[historyId="+historyId+"]"; }
}
