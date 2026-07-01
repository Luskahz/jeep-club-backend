package com.jeepclub.backend.health.infra.persistence.migration;

import com.jeepclub.backend.health.core.domain.BloodType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Order(0)
public class DependentMedicalProfileMigrationRunner implements ApplicationRunner {

    private static final String DEPENDENTS_TABLE = "membership_dependents";
    private static final String MEDICAL_PROFILES_TABLE = "medical_profiles";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!tableExists(DEPENDENTS_TABLE)
                || !tableExists(MEDICAL_PROFILES_TABLE)
                || !columnExists(DEPENDENTS_TABLE, "blood_type")) {
            return;
        }

        jdbcTemplate.query("""
                        SELECT id, blood_type, allergies, chronic_diseases, medications,
                               medical_notes, created_at, updated_at
                          FROM membership_dependents
                         WHERE (
                               NULLIF(TRIM(COALESCE(blood_type, '')), '') IS NOT NULL
                            OR NULLIF(TRIM(COALESCE(allergies, '')), '') IS NOT NULL
                            OR NULLIF(TRIM(COALESCE(chronic_diseases, '')), '') IS NOT NULL
                            OR NULLIF(TRIM(COALESCE(medications, '')), '') IS NOT NULL
                            OR NULLIF(TRIM(COALESCE(medical_notes, '')), '') IS NOT NULL
                         )
                           AND NOT EXISTS (
                               SELECT 1
                                 FROM medical_profiles mp
                                WHERE mp.owner_type = 'DEPENDENT'
                                  AND mp.owner_id = membership_dependents.id
                           )
                        """,
                rs -> {
                    Instant now = Instant.now();
                    Instant createdAt = rs.getTimestamp("created_at") == null
                            ? now
                            : rs.getTimestamp("created_at").toInstant();
                    Instant updatedAt = rs.getTimestamp("updated_at") == null
                            ? now
                            : rs.getTimestamp("updated_at").toInstant();

                    jdbcTemplate.update("""
                                    INSERT INTO medical_profiles (
                                        owner_type,
                                        owner_id,
                                        blood_type,
                                        allergies,
                                        chronic_conditions,
                                        continuous_medications,
                                        observations,
                                        created_at,
                                        updated_at
                                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                                    """,
                            "DEPENDENT",
                            rs.getLong("id"),
                            normalizeBloodType(rs.getString("blood_type")).name(),
                            clean(rs.getString("allergies")),
                            clean(rs.getString("chronic_diseases")),
                            clean(rs.getString("medications")),
                            clean(rs.getString("medical_notes")),
                            Timestamp.from(createdAt),
                            Timestamp.from(updatedAt)
                    );
                });

        dropLegacyColumnIfExists("blood_type");
        dropLegacyColumnIfExists("allergies");
        dropLegacyColumnIfExists("chronic_diseases");
        dropLegacyColumnIfExists("medications");
        dropLegacyColumnIfExists("medical_notes");
    }

    private boolean tableExists(String tableName) throws Exception {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            return objectExists(metadata.getTables(null, null, tableName, null))
                    || objectExists(metadata.getTables(null, null, tableName.toUpperCase(), null));
        }
    }

    private boolean columnExists(String tableName, String columnName) throws Exception {
        try (var connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            return objectExists(metadata.getColumns(null, null, tableName, columnName))
                    || objectExists(metadata.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase()));
        }
    }

    private boolean objectExists(ResultSet resultSet) throws Exception {
        try (resultSet) {
            return resultSet.next();
        }
    }

    private void dropLegacyColumnIfExists(String columnName) throws Exception {
        if (!columnExists(DEPENDENTS_TABLE, columnName)) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE " + DEPENDENTS_TABLE + " DROP COLUMN " + columnName);
    }

    private BloodType normalizeBloodType(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return BloodType.UNKNOWN;
        }

        String normalized = cleaned.toUpperCase()
                .replace("+", "_POSITIVE")
                .replace("-", "_NEGATIVE");

        try {
            return BloodType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return BloodType.UNKNOWN;
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }
}
