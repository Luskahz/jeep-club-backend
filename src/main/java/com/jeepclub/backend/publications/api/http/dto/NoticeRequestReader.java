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
public class NoticeRequestReader {
    private static final Set<String> FIELDS = Set.of("title", "content", "images");
    private static final Set<String> IMAGE_FIELDS = Set.of("storageKey", "position", "primary");
    private final ObjectMapper mapper;
    private final Validator validator;

    public CreateNoticeRequestDTO create(JsonNode raw) {
        checkShape(raw);
        CreateNoticeRequestDTO value = convert(raw, CreateNoticeRequestDTO.class);
        validate(value);
        return value;
    }

    public UpdateNoticeRequestDTO update(JsonNode raw) {
        checkShape(raw);
        UpdateNoticeRequestDTO value = convert(raw, UpdateNoticeRequestDTO.class);
        validate(value);
        return value;
    }

    private static void checkShape(JsonNode raw) {
        if (raw == null || !raw.isObject()) throw new MalformedNoticePayloadException("Notice body must be an object.");
        for (String field : raw.propertyNames()) {
            if (!FIELDS.contains(field)) throw new MalformedNoticePayloadException("Unsupported notice field: " + field);
        }
        for (String name : new String[]{"title", "content"}) {
            JsonNode value = raw.get(name);
            if (value != null && (!value.isString() || value.asString().isBlank())) {
                throw new MalformedNoticePayloadException(name + " must be non-blank text.");
            }
        }
        JsonNode images = raw.get("images");
        if (images != null) {
            if (!images.isArray()) throw new MalformedNoticePayloadException("images must be an array.");
            for (JsonNode image : images) {
                if (!image.isObject()) throw new MalformedNoticePayloadException("Each image must be an object.");
                for (String field : image.propertyNames()) {
                    if (!IMAGE_FIELDS.contains(field)) throw new MalformedNoticePayloadException("Unsupported image field: " + field);
                }
            }
        }
    }

    private <T> T convert(JsonNode raw, Class<T> type) {
        try {
            return mapper.treeToValue(raw, type);
        } catch (JacksonException exception) {
            throw new MalformedNoticePayloadException("Notice body could not be read.", exception);
        }
    }

    private <T> void validate(T value) {
        var violations = validator.validate(value);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
    }
}
