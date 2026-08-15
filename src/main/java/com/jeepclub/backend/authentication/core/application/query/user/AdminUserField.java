package com.jeepclub.backend.authentication.core.application.query.user;

public enum AdminUserField {

    ID,
    NAME,
    CPF,
    EMAIL,
    PHONE_NUMBER,
    STATUS,
    PASSWORD_CHANGE_REQUIRED,
    CREATED_AT,
    UPDATED_AT;

    public AdminUserProjectionField toProjectionField() {
        return AdminUserProjectionField.valueOf(name());
    }
}