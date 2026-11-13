package com.jeepclub.backend.dependents.core.application.query;

public enum AdminDependentField {
    ID("id"),
    NAME("name"),
    CPF("cpf"),
    BIRTH_DATE("birthDate"),
    RELATIONSHIP_TYPE("relationshipType"),
    PHONE_NUMBER("phoneNumber"),
    USER_ID("userId"),
    OWNER_NAME("ownerName"),
    STATUS("status"),
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
    ARCHIVED_AT("archivedAt"),
    PHOTO_URL("photoUrl");

    private final String property;

    AdminDependentField(String property) {
        this.property = property;
    }

    public String property() {
        return property;
    }
}
