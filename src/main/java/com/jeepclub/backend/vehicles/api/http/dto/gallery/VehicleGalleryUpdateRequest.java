package com.jeepclub.backend.vehicles.api.http.dto.gallery;

import com.jeepclub.backend.platform.storage.image.ImageUpload;
import com.jeepclub.backend.vehicles.core.application.VehicleGalleryChange;
import com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryRequest.Image;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(description="Substituição integral da galeria. Revisão atual e images são obrigatórios; [] limpa.",additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
public record VehicleGalleryUpdateRequest(@NotNull @PositiveOrZero Long expectedRevision,
        @NotNull @Size(max=5) List<@NotNull @Valid Image> images) {
    @com.fasterxml.jackson.annotation.JsonAnySetter
    public void unknown(String name,Object value) { throw new IllegalArgumentException("Unknown gallery field."); }
    public VehicleGalleryRequest asGallery() { return new VehicleGalleryRequest(expectedRevision,images); }
    public VehicleGalleryChange change(List<ImageUpload> uploads) { return asGallery().change(uploads); }
    @Override public String toString() { return "VehicleGalleryUpdateRequest[expectedRevision="+expectedRevision+"]"; }
}
