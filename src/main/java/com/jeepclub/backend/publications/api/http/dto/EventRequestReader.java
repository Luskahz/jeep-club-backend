package com.jeepclub.backend.publications.api.http.dto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.*;
import java.util.Set;

@Component @RequiredArgsConstructor
public class EventRequestReader {
    private final ObjectMapper mapper;
    public EventRequestDTO read(JsonNode raw, boolean create) {
        if (raw == null || !raw.isObject()) throw new IllegalArgumentException("Object required.");
        var allowed = Set.of("title", "content", "images", "startsAt", "endsAt", "charges");
        for (String field : raw.propertyNames()) {
            if (!allowed.contains(field) || raw.get(field).isNull() && !field.equals("endsAt")) throw new IllegalArgumentException("Invalid event field.");
        }
        if (create) for (String field : new String[]{"title","content","images","startsAt"})
            if (!raw.has(field)) throw new IllegalArgumentException("Required event field missing.");
        if (raw.has("charges")) {
            if (!raw.get("charges").isArray()) throw new IllegalArgumentException("Charges must be an array.");
            var fields = Set.of("chargeDefinitionId","name","description","amount","billingRequired","requiredForParticipation","participationCutoff");
            for (var charge : raw.get("charges")) {
                if (!charge.isObject()) throw new IllegalArgumentException("Charge must be an object.");
                for (String field : charge.propertyNames()) if (!fields.contains(field)) throw new IllegalArgumentException("Unsupported charge field.");
            }
        }
        return mapper.treeToValue(raw, EventRequestDTO.class);
    }
}
