package com.jeepclub.backend.storage.infra.persistence.mapper;

import com.jeepclub.backend.storage.core.domain.enums.*;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Portable VARCHAR enum storage; explicit named CHECKs live on the entities and in Flyway. */
public final class MediaEnumConverters {
    @Converter
    public static class MediaActorKindConverter implements AttributeConverter<MediaActorKind, String> {
        public String convertToDatabaseColumn(MediaActorKind value) { return value == null ? null : value.name(); }
        public MediaActorKind convertToEntityAttribute(String value) { return value == null ? null : MediaActorKind.valueOf(value); }
    }

    @Converter
    public static class MediaAuditOperationConverter implements AttributeConverter<MediaAuditOperation, String> {
        public String convertToDatabaseColumn(MediaAuditOperation value) { return value == null ? null : value.name(); }
        public MediaAuditOperation convertToEntityAttribute(String value) { return value == null ? null : MediaAuditOperation.valueOf(value); }
    }

    @Converter
    public static class MediaClassificationConverter implements AttributeConverter<MediaClassification, String> {
        public String convertToDatabaseColumn(MediaClassification value) { return value == null ? null : value.name(); }
        public MediaClassification convertToEntityAttribute(String value) { return value == null ? null : MediaClassification.valueOf(value); }
    }

    @Converter
    public static class MediaContextConverter implements AttributeConverter<MediaContext, String> {
        public String convertToDatabaseColumn(MediaContext value) { return value == null ? null : value.name(); }
        public MediaContext convertToEntityAttribute(String value) { return value == null ? null : MediaContext.valueOf(value); }
    }

    @Converter
    public static class MediaOriginConverter implements AttributeConverter<MediaOrigin, String> {
        public String convertToDatabaseColumn(MediaOrigin value) { return value == null ? null : value.name(); }
        public MediaOrigin convertToEntityAttribute(String value) { return value == null ? null : MediaOrigin.valueOf(value); }
    }

    @Converter
    public static class MediaPhysicalStateConverter implements AttributeConverter<MediaPhysicalState, String> {
        public String convertToDatabaseColumn(MediaPhysicalState value) { return value == null ? null : value.name(); }
        public MediaPhysicalState convertToEntityAttribute(String value) { return value == null ? null : MediaPhysicalState.valueOf(value); }
    }

    @Converter
    public static class MediaSubjectTypeConverter implements AttributeConverter<MediaSubjectType, String> {
        public String convertToDatabaseColumn(MediaSubjectType value) { return value == null ? null : value.name(); }
        public MediaSubjectType convertToEntityAttribute(String value) { return value == null ? null : MediaSubjectType.valueOf(value); }
    }

    @Converter
    public static class MediaVariantRoleConverter implements AttributeConverter<MediaVariantRole, String> {
        public String convertToDatabaseColumn(MediaVariantRole value) { return value == null ? null : value.name(); }
        public MediaVariantRole convertToEntityAttribute(String value) { return value == null ? null : MediaVariantRole.valueOf(value); }
    }

    private MediaEnumConverters() {}
}
