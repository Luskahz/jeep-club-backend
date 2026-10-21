package com.jeepclub.backend.vehicles.core.domain.model;

import com.jeepclub.backend.vehicles.core.domain.exception.VehicleLifecycleConflictException;
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
    private VehicleGallery gallery;     // Canonical media; photo is derived for legacy readers.
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
    private Instant archivedAt;
    private boolean legacyDeleted;
    private long galleryRevision;


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
                VehicleGallery.legacy(photo),
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
                null,
                null,
                false,
                0L
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
        return reconstitute(id, nickname, photo, plate, renavam, brand, model, manufacturingYear, modelYear, color, seatingCapacity, fuelType, engineDisplacement, status, towing, ownerId, createdAt, updatedAt, null, false);
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
            Instant archivedAt,
            boolean legacyDeleted
    ) {
        if ((status == VehicleStatus.ARCHIVED) != (archivedAt != null)) {
            throw new IllegalArgumentException("Archived state requires its archival timestamp only.");
        }
        if (archivedAt != null && (createdAt == null || archivedAt.isBefore(createdAt))) {
            throw new IllegalArgumentException("archivedAt cannot be before createdAt.");
        }
        if (legacyDeleted && status != VehicleStatus.ARCHIVED) {
            throw new IllegalArgumentException("Historically deleted vehicle must remain archived.");
        }

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
        vehicle.gallery = VehicleGallery.legacy(photo);
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
        vehicle.archivedAt = archivedAt;
        vehicle.legacyDeleted = legacyDeleted;
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
        if (status != VehicleStatus.ACTIVE) {
            throw new VehicleLifecycleConflictException("Only active vehicles can be edited.");
        }
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

        if (!Objects.equals(getPhoto(),photo)) {
            assertLegacyPhotoChangeAllowed(photo);
            this.gallery=VehicleGallery.legacy(photo);
            this.galleryRevision++;
        }
        this.nickname = nickname;
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


    public String getPhoto() { return gallery.primaryReference(); }
    public java.util.List<VehicleImage> getImages() { return gallery.images(); }

    /** Persistence-only reconstruction, after the ordinary aggregate invariants were checked. */
    public static Vehicle restoreGallery(Vehicle vehicle,VehicleGallery gallery,long revision) {
        validateId(vehicle.id);
        if(revision<0) throw new IllegalArgumentException("Gallery revision cannot be negative.");
        vehicle.gallery=Objects.requireNonNull(gallery);vehicle.galleryRevision=revision;
        return vehicle;
    }

    public void initializeGallery(VehicleGallery gallery) {
        if(id!=null) throw new IllegalStateException("Only a new vehicle can initialize its gallery.");
        this.gallery=Objects.requireNonNull(gallery);
    }

    public boolean replaceGallery(VehicleGallery gallery,long expectedRevision,Instant now) {
        if(status!=VehicleStatus.ACTIVE || legacyDeleted) throw new VehicleLifecycleConflictException("Only active vehicles can change their gallery.");
        Objects.requireNonNull(gallery);Objects.requireNonNull(now);
        validateUpdatedAtNotBeforeCreatedAt(createdAt,now);
        if(this.gallery.equals(gallery)) return false;
        requireGalleryRevision(expectedRevision);
        this.gallery=gallery;this.galleryRevision++;this.updatedAt=now;return true;
    }

    public void requireGalleryRevision(long expectedRevision) {
        if(expectedRevision!=galleryRevision) throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleGalleryConflictException("Vehicle gallery changed; reload it before retrying.");
    }

    /** A version-unaware writer cannot overwrite an aggregate that has already changed its gallery. */
    public void assertLegacyPhotoChangeAllowed(String reference) {
        if(!Objects.equals(getPhoto(),reference) && (gallery.images().size()>1 || galleryRevision!=0))
            throw new com.jeepclub.backend.vehicles.core.domain.exception.VehicleGalleryConflictException("Use the versioned gallery contract to change a multiple or previously edited gallery.");
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

    public boolean archive(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");
        validateUpdatedAtNotBeforeCreatedAt(createdAt, now);
        if (status == VehicleStatus.ARCHIVED) return false;
        status = VehicleStatus.ARCHIVED;
        archivedAt = now;
        updatedAt = now;
        return true;
    }

    public boolean unarchive(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");
        validateUpdatedAtNotBeforeCreatedAt(createdAt, now);
        if (legacyDeleted) throw new VehicleLifecycleConflictException("Historically deleted vehicle cannot be restored.");
        if (status == VehicleStatus.INACTIVE) return false;
        if (status != VehicleStatus.ARCHIVED) {
            throw new VehicleLifecycleConflictException("Only an archived record can be unarchived.");
        }
        status = VehicleStatus.INACTIVE;
        archivedAt = null;
        updatedAt = now;
        return true;
    }

    private void assertNotArchived() {
        if (status == VehicleStatus.ARCHIVED) {
            throw new VehicleLifecycleConflictException("Archived record requires explicit unarchive before this operation.");
        }
    }

    public boolean activate(Instant now) {
        assertNotArchived();
        Objects.requireNonNull(now, "now cannot be null");
        validateUpdatedAtNotBeforeCreatedAt(createdAt, now);
        if (status == VehicleStatus.ACTIVE) return false;
        status = VehicleStatus.ACTIVE;
        updatedAt = now;
        return true;
    }

    public boolean deactivate(Instant now) {
        assertNotArchived();
        Objects.requireNonNull(now, "now cannot be null");
        validateUpdatedAtNotBeforeCreatedAt(createdAt, now);
        if (status == VehicleStatus.INACTIVE) return false;
        status = VehicleStatus.INACTIVE;
        updatedAt = now;
        return true;
    }
}
