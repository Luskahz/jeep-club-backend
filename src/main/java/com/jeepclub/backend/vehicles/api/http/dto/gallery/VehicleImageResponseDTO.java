package com.jeepclub.backend.vehicles.api.http.dto.gallery;
import com.jeepclub.backend.vehicles.core.domain.model.VehicleImage;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description="Imagem da galeria. storageKey é referência pública ou valor legado preservado, nunca path físico de provider. Nova foto usa arquivo multipart.")
public record VehicleImageResponseDTO(String storageKey,int position,boolean primary) {
    public static VehicleImageResponseDTO from(VehicleImage i) { return new VehicleImageResponseDTO(i.storageKey(),i.position(),i.primary()); }
    @Override public String toString() { return "VehicleImageResponseDTO[position="+position+", primary="+primary+"]"; }
}
