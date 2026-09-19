package com.jeepclub.backend.vehicles.core.domain.enums;

public enum VehicleStatus {
    ACTIVE,

    /**
     * Legado de compatibilidade de leitura. Nenhum caso de uso atual produz
     * este valor — a exclusão vigente é hard delete com snapshot histórico
     * (ver {@code vehicles_vehicle_history}), não soft delete. O valor é
     * mantido somente para que uma linha pré-existente com esse status ainda
     * seja lida sem falha de mapeamento; {@code findActiveVehicle} nos
     * services trata qualquer status diferente de {@code ACTIVE} como
     * veículo não encontrado (404), então um registro legado nunca fica
     * visível ou editável pelas rotas atuais.
     */
    SOFT_DELETED
}
