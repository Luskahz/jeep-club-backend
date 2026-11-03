package com.jeepclub.backend.tools.core.application.query;

/** Fields of the existing administrative summary; no detail-only data is exposed. */
public enum AdminToolField {
    ID("id"), NAME("name"), STATUS("status"), USER_ID("userId");
    private final String property;
    AdminToolField(String property) { this.property = property; }
    public String property() { return property; }
}
