package com.jeepclub.backend.vehicles.core.domain.model;

import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Vehicle {
    private Long id;
    private String nickname;
    private String photo;
    private String plate;               // Placa do veículo
    private String renavam;             // Código RENAVAM (específico para o Brasil)
    private String brand;               // Marca (ex: Toyota, Ford)
    private String model;               // Modelo (ex: Hilux, SW4)
    private int manufacturingYear;      // Ano de fabricação
    private int modelYear;              // Ano do modelo
    private String color;               // Cor predominante
    private int seatingCapacity;        // Capacidade de passageiros (ex: 5, 7, 8)
    private String fuelType;            // Tipo de combustível (Gasolina, Flex, Diesel, Elétrico)
    private double engineDisplacement;
    private VehicleStatus status;  // Cilindrada/Motor (ex: 2.0, 3.0)
    private Long ownerId;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant disabledAt;


    public Vehicle(
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
            String fuelType,
            double engineDisplacement,
            Long ownerId
    ) {
        this.nickname = nickname;
        this.photo = photo;
        this.plate = plate;
        this.renavam = renavam;
        this.brand = brand;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.modelYear = modelYear;
        this.color = color;
        this.seatingCapacity = seatingCapacity;
        this.fuelType = fuelType;
        this.engineDisplacement = engineDisplacement;
        this.ownerId = ownerId;


        this.status = VehicleStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
