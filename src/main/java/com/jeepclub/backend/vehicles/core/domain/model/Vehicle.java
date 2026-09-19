package com.jeepclub.backend.vehicles.core.domain.model;

import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Vehicle {
    private Long id;
    private String nickname;            // Nome que o membro da para o proprio veiculo
    private String photo;               // Foto unica
    private String plate;               // Placa do veículo
    private String renavam;             // Código RENAVAM (específico para o Brasil)
    private String brand;               // Marca (ex: Toyota, Ford)
    private String model;               // Modelo (ex: Hilux, SW4)
    private int manufacturingYear;      // Ano de fabricação
    private int modelYear;              // Ano do modelo
    private String color;               // Cor predominante
    private int seatingCapacity;        // Capacidade de passageiros (ex: 5, 7, 8)
    private FuelType fuelType;          // Tipo de combustível (Gasolina, Flex, Diesel, Elétrico)
    private double engineDisplacement;  // Cilindrada/Motor (ex: 2.0, 3.0)
    private VehicleStatus status;
    private Boolean towing;

    private Long ownerId;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;


    public static Vehicle create(
            String nickname,
            String photo,
            String plate,
            String renavam,
            String brand,
            String model,
            int manufacturingYear,
            int modelYear,
            String color,
            int seatingCapacity,
            FuelType fuelType,
            double engineDisplacement,
            Boolean towing,
            Long ownerId,
            Instant createdAt
    ) {
        return new Vehicle(
                null,
                nickname,
                photo,
                normalizePlate(plate),
                normalizeRenavam(renavam),
                brand,
                model,
                manufacturingYear,
                modelYear,
                color,
                seatingCapacity,
                fuelType,
                engineDisplacement,
                VehicleStatus.ACTIVE,
                towing,
                ownerId,
                createdAt,
                null,
                null
        );

    }

    public static Vehicle reconstitute(
            Long id,
            String nickname,
            String photo,
            String plate,
            String renavam,
            String brand,
            String model,
            int manufacturingYear,
            int modelYear,
            String color,
            int seatingCapacity,
            FuelType fuelType,
            double engineDisplacement,
            VehicleStatus status,
            Boolean towing,
            Long ownerId,
            Instant createdAt,
            Instant updatedAt,
            Instant disabledAt
    ) {
        Vehicle vehicle = new Vehicle();
        vehicle.id = id;
        vehicle.nickname = nickname;
        vehicle.photo = photo;
        vehicle.plate = normalizePlate(plate);
        vehicle.renavam = normalizeRenavam(renavam);
        vehicle.brand = brand;
        vehicle.model = model;
        vehicle.manufacturingYear = manufacturingYear;
        vehicle.modelYear = modelYear;
        vehicle.color = color;
        vehicle.seatingCapacity = seatingCapacity;
        vehicle.fuelType = fuelType;
        vehicle.engineDisplacement = engineDisplacement;
        vehicle.status = status;
        vehicle.towing = towing;
        vehicle.ownerId = ownerId;
        vehicle.createdAt = createdAt;
        vehicle.updatedAt = updatedAt;
        vehicle.deletedAt = disabledAt;
        return vehicle;
    }

    public Vehicle softDelete(Instant now) {
        this.status = VehicleStatus.SOFT_DELETED;
        this.updatedAt = now;
        this.deletedAt = now;
        return this;
    }

    public void update(
            String nickname,
            String photo,
            String plate,
            String renavam,
            String brand,
            String model,
            int manufacturingYear,
            int modelYear,
            String color,
            int seatingCapacity,
            FuelType fuelType,
            double engineDisplacement,
            Boolean towing,
            Instant now
    ) {
        this.nickname = nickname;
        this.photo = photo;
        this.plate = normalizePlate(plate);
        this.renavam = normalizeRenavam(renavam);
        this.brand = brand;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.modelYear = modelYear;
        this.color = color;
        this.seatingCapacity = seatingCapacity;
        this.fuelType = fuelType;
        this.engineDisplacement = engineDisplacement;
        this.towing = towing;
        this.updatedAt = now;
    }

    /**
     * Forma canônica de placa: trim + uppercase. Aplicada em create/update/
     * reconstitute para que persistência, consultas de duplicidade e
     * comparações no domínio nunca dependam do chamador já enviar a forma
     * canônica.
     */
    public static String normalizePlate(String rawPlate) {
        return rawPlate == null ? null : rawPlate.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Forma canônica de RENAVAM: somente dígitos. O checksum em
     * {@code RenavamValidator} é calculado sobre este mesmo resultado, então
     * validação e persistência nunca divergem sobre o que conta como dígito.
     */
    public static String normalizeRenavam(String rawRenavam) {
        return rawRenavam == null ? null : rawRenavam.replaceAll("\\D", "");
    }
}
