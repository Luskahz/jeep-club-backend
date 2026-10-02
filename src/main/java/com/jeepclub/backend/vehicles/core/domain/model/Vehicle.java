package com.jeepclub.backend.vehicles.core.domain.model;

import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Vehicle {

    private static final int MIN_YEAR = 1900;

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
        validateOwnerId(ownerId);
        requireText(brand, "brand");
        requireText(model, "model");
        requireText(color, "color");
        validateYear(manufacturingYear, "manufacturingYear");
        validateYear(modelYear, "modelYear");
        validateSeatingCapacity(seatingCapacity);
        validateEngineDisplacement(engineDisplacement);
        Objects.requireNonNull(fuelType, "fuelType is required.");
        Objects.requireNonNull(towing, "towing is required.");
        Objects.requireNonNull(createdAt, "createdAt is required.");

        return new Vehicle(
                null,
                nickname,
                photo,
                requireCanonicalPlate(plate),
                requireCanonicalRenavam(renavam),
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
            Instant updatedAt
    ) {
        validateId(id);
        validateOwnerId(ownerId);
        requireText(brand, "brand");
        requireText(model, "model");
        validateYear(manufacturingYear, "manufacturingYear");
        validateYear(modelYear, "modelYear");
        validateSeatingCapacity(seatingCapacity);
        validateEngineDisplacement(engineDisplacement);
        Objects.requireNonNull(fuelType, "fuelType is required.");
        Objects.requireNonNull(status, "status is required.");
        Objects.requireNonNull(towing, "towing is required.");
        Objects.requireNonNull(createdAt, "createdAt is required.");
        validateUpdatedAtNotBeforeCreatedAt(createdAt, updatedAt);

        Vehicle vehicle = new Vehicle();
        vehicle.id = id;
        vehicle.nickname = nickname;
        vehicle.photo = photo;
        vehicle.plate = requireCanonicalPlate(plate);
        vehicle.renavam = requireCanonicalRenavam(renavam);
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
        return vehicle;
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
        requireText(brand, "brand");
        requireText(model, "model");
        validateYear(manufacturingYear, "manufacturingYear");
        validateYear(modelYear, "modelYear");
        validateSeatingCapacity(seatingCapacity);
        validateEngineDisplacement(engineDisplacement);
        Objects.requireNonNull(fuelType, "fuelType is required.");
        Objects.requireNonNull(towing, "towing is required.");
        Objects.requireNonNull(now, "now is required.");
        validateUpdatedAtNotBeforeCreatedAt(this.createdAt, now);

        this.nickname = nickname;
        this.photo = photo;
        this.plate = requireCanonicalPlate(plate);
        this.renavam = requireCanonicalRenavam(renavam);
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

    private static String requireCanonicalPlate(String rawPlate) {
        String canonicalPlate = normalizePlate(rawPlate);
        if (canonicalPlate == null || canonicalPlate.isBlank()) {
            throw new IllegalArgumentException("plate is required.");
        }
        return canonicalPlate;
    }

    private static String requireCanonicalRenavam(String rawRenavam) {
        String canonicalRenavam = normalizeRenavam(rawRenavam);
        if (canonicalRenavam == null || canonicalRenavam.isBlank()) {
            throw new IllegalArgumentException("renavam is required.");
        }
        return canonicalRenavam;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
    }

    private static void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be positive.");
        }
    }

    private static void validateOwnerId(Long ownerId) {
        if (ownerId == null || ownerId <= 0) {
            throw new IllegalArgumentException("ownerId must be positive.");
        }
    }

    private static void validateYear(int year, String field) {
        if (year < MIN_YEAR) {
            throw new IllegalArgumentException(field + " must not be before " + MIN_YEAR + ".");
        }
    }

    private static void validateSeatingCapacity(int seatingCapacity) {
        if (seatingCapacity < 1) {
            throw new IllegalArgumentException("seatingCapacity must be at least 1.");
        }
    }

    private static void validateEngineDisplacement(double engineDisplacement) {
        if (engineDisplacement < 0) {
            throw new IllegalArgumentException("engineDisplacement cannot be negative.");
        }
    }

    private static void validateUpdatedAtNotBeforeCreatedAt(Instant createdAt, Instant updatedAt) {
        if (updatedAt != null && updatedAt.isBefore(createdAt)) {
            throw new IllegalStateException("updatedAt cannot be before createdAt.");
        }
    }
}
