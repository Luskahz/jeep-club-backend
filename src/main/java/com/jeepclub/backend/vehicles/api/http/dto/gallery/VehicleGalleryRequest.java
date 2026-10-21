package com.jeepclub.backend.vehicles.api.http.dto.gallery;
import com.jeepclub.backend.vehicles.core.application.VehicleGalleryChange;
import com.jeepclub.backend.platform.storage.image.ImageUpload;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(description="Galeria completa: [] limpa. expectedRevision é obrigatório na manutenção; omitido/zero na criação. Cada entrada usa storageKey retida/pública existente OU uploadIndex da part photos. Exatamente uma principal se não vazia.",additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
public record VehicleGalleryRequest(@PositiveOrZero Long expectedRevision,
        @NotNull @Size(max=5) List<@NotNull @Valid Image> images) {
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE)
    public record Image(@Schema(description="Referência retida ou pública existente. O teto UTF-16 acomoda 500 caracteres MySQL legados, inclusive suplementares; novas referências continuam estritamente globais.") @Size(max=1000) String storageKey,@Min(0) @Max(4) Integer uploadIndex,
            @NotNull @Min(0) @Max(4) Integer position,@NotNull Boolean primary) {
        @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String name,Object value) { throw new IllegalArgumentException("Unknown gallery image field."); }
        @Override public String toString() { return "GalleryImage[position="+position+", primary="+primary+"]"; }
    }
    @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String name,Object value) { throw new IllegalArgumentException("Unknown gallery field."); }
    public VehicleGalleryChange change(List<ImageUpload> uploads) {
        return new VehicleGalleryChange(expectedRevision,images.stream().map(i -> new VehicleGalleryChange.Entry(i.storageKey(),i.uploadIndex(),i.position(),i.primary())).toList(),uploads);
    }
    @Override public String toString() { return "VehicleGalleryRequest[expectedRevision="+expectedRevision+"]"; }
}
