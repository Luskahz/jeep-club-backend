package com.jeepclub.backend.vehicles.core.application.query;

public enum AdminVehicleField {
    ID("id"), NICKNAME("nickname"), PLATE("plate"), PHOTO("photo"), MODEL_YEAR("modelYear"),
    MODEL("model"), COLOR("color"), OWNER_ID("ownerId"), OWNER_NAME("ownerName"), STATUS("status");
    private final String property;
    AdminVehicleField(String property) {this.property=property;}
    public String property() {return property;}
}
