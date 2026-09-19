package com.jeepclub.backend.vehicles.api.http.dto.edit;

import com.jeepclub.backend.vehicles.core.application.FieldUpdate;
import com.jeepclub.backend.vehicles.core.application.VehicleEditFields;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

/**
 * Lê o corpo bruto do PUT de edição e monta um {@link VehicleEditFields},
 * distinguindo campo ausente de campo enviado como null. Cada valor presente
 * é convertido e validado pelas anotações de {@link EditRequestDTO} antes de
 * virar um {@link FieldUpdate}; presença/nullability por campo é decidida
 * separadamente pelo resolver da camada de aplicação.
 */
@Component
@RequiredArgsConstructor
public class EditRequestFieldReader {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public VehicleEditFields read(JsonNode raw) {
        if (raw == null || raw.isNull() || !raw.isObject()) {
            throw new MalformedEditPayloadException(
                    "The request body must be a JSON object.",
                    null
            );
        }

        EditRequestDTO dto = toDto(raw);
        validate(dto);

        return new VehicleEditFields(
                field(raw, "nickname", dto.nickname()),
                field(raw, "photo", dto.photo()),
                field(raw, "plate", dto.plate()),
                field(raw, "renavam", dto.renavam()),
                field(raw, "brand", dto.brand()),
                field(raw, "model", dto.model()),
                field(raw, "manufacturingYear", dto.manufacturingYear()),
                field(raw, "modelYear", dto.modelYear()),
                field(raw, "color", dto.color()),
                field(raw, "seatingCapacity", dto.seatingCapacity()),
                field(raw, "fuelType", dto.fuelType()),
                field(raw, "engineDisplacement", dto.engineDisplacement()),
                field(raw, "towing", dto.towing())
        );
    }

    private EditRequestDTO toDto(JsonNode raw) {
        try {
            return objectMapper.treeToValue(raw, EditRequestDTO.class);
        } catch (JacksonException exception) {
            throw new MalformedEditPayloadException(
                    "The request body could not be read.",
                    exception
            );
        }
    }

    private void validate(EditRequestDTO dto) {
        Set<ConstraintViolation<EditRequestDTO>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private static <T> FieldUpdate<T> field(JsonNode raw, String name, T convertedValue) {
        JsonNode node = raw.get(name);
        if (node == null) {
            return FieldUpdate.omitted();
        }
        if (node.isNull()) {
            return FieldUpdate.explicitNull();
        }
        return FieldUpdate.of(convertedValue);
    }
}
