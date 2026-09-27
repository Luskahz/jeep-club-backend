package com.jeepclub.backend.publications.api.http.dto;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ServiceRequestReader {
    private static final Set<String> FIELDS = Set.of("title", "content", "amount", "contactPhone", "images");
    private static final Set<String> IMAGE_FIELDS = Set.of("storageKey", "position", "primary");
    private final ObjectMapper mapper;
    private final Validator validator;

    public CreateServicePublicationRequestDTO create(JsonNode raw) {
        checkShape(raw, false);
        var value = convert(raw, CreateServicePublicationRequestDTO.class);
        validate(value);
        return value;
    }

    public UpdateServicePublicationRequestDTO update(JsonNode raw) {
        checkShape(raw, true);
        var value = convert(raw, UpdateServicePublicationRequestDTO.class);
        validate(value);
        return value;
    }

    private static void checkShape(JsonNode raw, boolean partial) {
        if (raw == null || !raw.isObject()) throw new MalformedServicePayloadException("Service body must be an object.");
        if (partial && raw.isEmpty()) throw new MalformedServicePayloadException("At least one change is required.");
        for (String field : raw.propertyNames()) {
            if (!FIELDS.contains(field)) throw new MalformedServicePayloadException("Unsupported service field: " + field);
        }
        for (String field : new String[]{"title", "content", "contactPhone"}) {
            JsonNode value = raw.get(field);
            if (value != null && (!value.isString() || value.asString().isBlank()))
                throw new MalformedServicePayloadException(field + " must be non-blank text.");
        }
        JsonNode amount = raw.get("amount");
        if (amount != null && !amount.isNumber()) throw new MalformedServicePayloadException("amount must be a number.");
        JsonNode images = raw.get("images");
        if (images != null) {
            if (!images.isArray()) throw new MalformedServicePayloadException("images must be an array.");
            for (JsonNode image : images) {
                if (!image.isObject()) throw new MalformedServicePayloadException("Each image must be an object.");
                for (String field : image.propertyNames()) {
                    if (!IMAGE_FIELDS.contains(field))
                        throw new MalformedServicePayloadException("Unsupported image field: " + field);
                }
            }
        }
    }

    private <T> T convert(JsonNode raw, Class<T> type) {
        try { return mapper.treeToValue(raw, type); }
        catch (JacksonException exception) { throw new MalformedServicePayloadException("Service body could not be read.", exception); }
    }

    private <T> void validate(T value) {
        var violations = validator.validate(value);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
    }
}
