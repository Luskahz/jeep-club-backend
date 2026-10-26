package com.jeepclub.backend.platform.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Runs only with -Pmysql-migrations; missing Docker is a FAILURE, never a skip. */
@Testcontainers
class MySqlMigrationIT {
    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.46")
            .withUsername("root").withPassword("migration-test-only")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci");

    @Test void numericRegistryUpgradePreservesIdentityAndAllLegacyClaimEvidence() throws Exception {
        String db=legacyRegistryDatabase();var identitiesBefore=rows(db,"select * from identity_users order by id");
        var auditBefore=rows(db,"select * from membership_member_audits where id=900");
        var claimEvidence=rows(db,"select member_id,membership_number,actor_user_id,claimed_at,reason from membership_number_claims order by membership_number");
        flyway(db).migrate();validateJpa(db);
        assertThat(rows(db,"select * from identity_users order by id")).isEqualTo(identitiesBefore);
        assertThat(rows(db,"select * from membership_member_audits where id=900")).isEqualTo(auditBefore);
        assertThat(rows(db,"select member_id,new_number,actor_user_id,occurred_at,reason from membership_member_audits where operation='LEGACY_CLAIM_SNAPSHOT' order by new_number")).isEqualTo(claimEvidence);
        assertThat(rows(db,"select id,identity_user_id,membership_number,version from membership_members")).containsExactly("[800, 700, 27, 5]");
        assertThat(count(db,"membership_number_claims")).isEqualTo(2);
        assertThat(rows(db,"select membership_number,member_id from membership_number_claims order by membership_number"))
                .containsExactly("[27, 800]","[28, null]");
        assertThat(rows(db,"select new_number,reason from membership_member_audits where operation='LEGACY_CLAIM_SNAPSHOT' order by new_number"))
                .containsExactly("[00028, explicit reservation]","[020, retired numeric]","[027, current number]","[OLD-X, retired opaque]");
        assertThat(rows(db,"select next_value,format_signature from membership_registry_sequence where id=1")).containsExactly("[null, null]");
        // An old number can be occupied again without deleting any original audit.
        try(var c=connection(db);var st=c.createStatement()) {
            st.executeUpdate("INSERT INTO membership_members(id,identity_user_id,membership_number,created_at,updated_at,version) VALUES(801,701,'20',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)");
            st.executeUpdate("INSERT INTO membership_number_claims VALUES(SHA2('numeric:20',256),'20',801,701,UTC_TIMESTAMP(6),'reused current number')");
        }
        assertThat(count(db,"membership_member_audits")).isEqualTo(5);
        assertThat(flyway(db).migrate().migrationsExecuted).isZero();
    }

    @ParameterizedTest @ValueSource(strings={"OPAQUE_CURRENT","ZERO_CURRENT","OPAQUE_RESERVATION","CANONICAL_MEMBERS_COLLISION","CURRENT_RESERVATION_COLLISION","MISSING_CLAIM","CLAIM_LITERAL_MISMATCH"})
    void numericRegistryPreflightRejectsIncompatibleDataWithoutBusinessMutations(String defect) throws Exception {
        String db=legacyRegistryDatabase();
        try(var c=connection(db);var st=c.createStatement()) {
            switch(defect) {
                case "OPAQUE_CURRENT" -> st.executeUpdate("update membership_members set membership_number='A-27' where id=800");
                case "ZERO_CURRENT" -> st.executeUpdate("update membership_members set membership_number='000' where id=800");
                case "OPAQUE_RESERVATION" -> st.executeUpdate("update membership_number_claims set membership_number='RESERVED' where member_id is null");
                case "CANONICAL_MEMBERS_COLLISION" -> st.executeUpdate("insert into membership_members values(801,701,'27',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)");
                case "CURRENT_RESERVATION_COLLISION" -> st.executeUpdate("update membership_number_claims set membership_number='27' where member_id is null");
                case "MISSING_CLAIM" -> st.executeUpdate("delete from membership_number_claims where membership_number='027'");
                case "CLAIM_LITERAL_MISMATCH" -> st.executeUpdate("update membership_number_claims set membership_number='027 ' where membership_number='027'");
                default -> throw new IllegalArgumentException(defect);
            }
        }
        var membersBefore=rows(db,"select * from membership_members order by id");
        var claimsBefore=rows(db,"select * from membership_number_claims order by claim_key");
        var auditsBefore=rows(db,"select * from membership_member_audits order by id");
        var identitiesBefore=rows(db,"select * from identity_users order by id");
        var sequenceBefore=rows(db,"select * from membership_registry_sequence");
        assertThatThrownBy(() -> flyway(db).migrate()).isInstanceOf(FlywayException.class).hasStackTraceContaining("BACK-526 preflight");
        assertThat(rows(db,"select * from membership_members order by id")).isEqualTo(membersBefore);
        assertThat(rows(db,"select * from membership_number_claims order by claim_key")).isEqualTo(claimsBefore);
        assertThat(rows(db,"select * from membership_member_audits order by id")).isEqualTo(auditsBefore);
        assertThat(rows(db,"select * from identity_users order by id")).isEqualTo(identitiesBefore);
        assertThat(rows(db,"select * from membership_registry_sequence")).isEqualTo(sequenceBefore);
    }

    private static String legacyRegistryDatabase() throws Exception {
        String db=database();
        Flyway.configure().dataSource(url(db),MYSQL.getUsername(),MYSQL.getPassword()).locations("classpath:db/migration").target("9").cleanDisabled(true).load().migrate();
        try(var c=connection(db);var st=c.createStatement()) {
            st.executeUpdate("insert into identity_users(id,name,cpf,status,created_at) values(700,'Synthetic member','00000000000','ACTIVE',UTC_TIMESTAMP(6)),(701,'Synthetic actor','00000000001','ACTIVE',UTC_TIMESTAMP(6))");
            st.executeUpdate("insert into membership_members values(800,700,'027',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),4)");
            st.executeUpdate("insert into membership_number_claims values(SHA2('numeric:27',256),'027',800,701,'2026-10-01 00:00:00','current number'),(SHA2('numeric:20',256),'020',800,701,'2026-09-01 00:00:00','retired numeric'),(SHA2('literal:OLD-X',256),'OLD-X',800,701,'2026-08-01 00:00:00','retired opaque'),(SHA2('numeric:28',256),'00028',null,701,'2026-10-01 00:00:00','explicit reservation')");
            st.executeUpdate("insert into membership_member_audits values(900,800,null,'027',701,'2026-10-01 00:00:00','original immutable audit','ASSIGN','SUCCESS')");
            st.executeUpdate("update membership_registry_sequence set next_value=99999,format_signature='v1:100:3:' where id=1");
        }
        return db;
    }

    @Test void onboardingReviewMigrationPreservesLegacyApprovalAndIndependentEvidenceOnReplay() throws Exception {
        String db = database();
        Flyway.configure().dataSource(url(db), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration").target("5").cleanDisabled(true).load().migrate();
        try (var c = connection(db); var statement = c.createStatement()) {
            statement.executeUpdate("INSERT INTO membership_applications(id,name,cpf,status,created_user_id,reviewed_by_user_id,requested_at,reviewed_at,version) "
                    + "VALUES(700,'Synthetic legacy approval','00000000000','APPROVED',701,702,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)");
        }
        var before = fixtureRows(db, "membership_applications");
        flyway(db).migrate(); validateJpa(db);
        assertThat(fixtureRows(db, "membership_applications")).isEqualTo(before);
        assertThat(count(db, "membership_application_review_history")).isZero();
        try (var c = connection(db); var statement = c.createStatement()) {
            statement.executeUpdate("INSERT INTO membership_application_review_history(application_id,operation,previous_status,resulting_status,"
                    + "previous_reviewer_id,previous_reviewed_at,original_user_id,actor_id,occurred_at) "
                    + "VALUES(700,'REVERSE_APPROVAL','APPROVED','REJECTED',702,UTC_TIMESTAMP(6),701,703,UTC_TIMESTAMP(6))");
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(c,
                    new org.springframework.core.io.ClassPathResource("db/migration/V6__membership_onboarding_review_history.sql"));
            statement.executeUpdate("DELETE FROM membership_applications WHERE id=700");
        }
        assertThat(count(db, "membership_application_review_history")).isEqualTo(1);
        assertThat(flyway(db).migrate().migrationsExecuted).isZero();
    }

    @Test void vehicleGalleryMigrationPreservesLegacyLiveAndHistoricalPhotosAndControlledReplay() throws Exception {
        String db=database();
        Flyway.configure().dataSource(url(db),MYSQL.getUsername(),MYSQL.getPassword()).locations("classpath:db/migration")
                .target("7").cleanDisabled(true).load().migrate();
        var photos=java.util.Arrays.asList("images/2026/10/19/00000000-0000-0000-0000-000000000001.png","https://legacy.invalid/Photo.PNG?old=1",null,"");
        try(var c=connection(db);var st=c.createStatement()) {
            for(int i=0;i<4;i++) {
                String value=photos.get(i)==null?"NULL":"'"+photos.get(i)+"'";
                st.executeUpdate("INSERT INTO vehicles_vehicle(id,photo,plate,renavam,brand,model,manufacturing_year,model_year,seating_capacity,fuel_type,engine_displacement,status,towing,owner_id,created_at) VALUES("+(900+i)+","+value+",'MIG000"+i+"','0000000090"+i+"','Jeep','Legacy',2020,2021,5,'DIESEL',2,'ACTIVE',0,800,UTC_TIMESTAMP(6))");
                st.executeUpdate("INSERT INTO vehicles_vehicle_history(id,vehicle_id,photo,plate,renavam,brand,model,manufacturing_year,model_year,seating_capacity,fuel_type,engine_displacement,status,towing,owner_id,created_at,deleted_by_user_id,deleted_at) VALUES("+(950+i)+","+(900+i)+","+value+",'MIG000"+i+"','0000000090"+i+"','Jeep','Legacy',2020,2021,5,'DIESEL',2,'ACTIVE',0,800,UTC_TIMESTAMP(6),800,UTC_TIMESTAMP(6))");
            }
        }
        var liveColumns=fixtureColumns(db,"vehicles_vehicle");var historyColumns=fixtureColumns(db,"vehicles_vehicle_history");
        var liveBefore=fixtureRows(db,"vehicles_vehicle",liveColumns);var historyBefore=fixtureRows(db,"vehicles_vehicle_history",historyColumns);
        flyway(db).migrate();validateJpa(db);
        assertThat(fixtureRows(db,"vehicles_vehicle",liveColumns)).isEqualTo(liveBefore);
        assertThat(fixtureRows(db,"vehicles_vehicle_history",historyColumns)).isEqualTo(historyBefore);
        var liveImages=rows(db,"SELECT vehicle_id,storage_key,position,CAST(is_primary AS UNSIGNED) FROM vehicle_images ORDER BY vehicle_id");
        var historyImages=rows(db,"SELECT history_id,storage_key,position,CAST(is_primary AS UNSIGNED) FROM vehicle_image_history ORDER BY history_id");
        assertThat(liveImages).containsExactly("[900, "+photos.get(0)+", 0, 1]","[901, "+photos.get(1)+", 0, 1]","[903, , 0, 1]");
        assertThat(historyImages).containsExactly("[950, "+photos.get(0)+", 0, 1]","[951, "+photos.get(1)+", 0, 1]","[953, , 0, 1]");
        try(var c=connection(db);var st=c.createStatement()) {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(c,new org.springframework.core.io.ClassPathResource("db/migration/V8__vehicle_gallery.sql"));
            assertThat(rows(db,"SELECT vehicle_id,storage_key,position,CAST(is_primary AS UNSIGNED) FROM vehicle_images ORDER BY vehicle_id")).isEqualTo(liveImages);
            st.executeUpdate("INSERT INTO vehicle_images(vehicle_id,storage_key,position,is_primary) VALUES(901,'images/2026/10/19/00000000-0000-0000-0000-000000000002.png',1,b'0')");
            assertThatThrownBy(()->st.executeUpdate("INSERT INTO vehicle_images(vehicle_id,storage_key,position,is_primary) VALUES(901,'new-reference',5,b'0')")).isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(()->st.executeUpdate("INSERT INTO vehicle_images(vehicle_id,storage_key,position,is_primary) VALUES(901,'new-reference',0,b'0')")).isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(()->st.executeUpdate("INSERT INTO vehicle_images(vehicle_id,storage_key,position,is_primary) VALUES(901,'https://legacy.invalid/Photo.PNG?old=1',2,b'0')")).isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(()->st.executeUpdate("DELETE FROM vehicles_vehicle WHERE id=901")).isInstanceOf(java.sql.SQLException.class);
            st.executeUpdate("DELETE FROM vehicle_images WHERE vehicle_id=900");st.executeUpdate("UPDATE vehicles_vehicle SET photo=NULL,gallery_revision=1 WHERE id=900");
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(c,new org.springframework.core.io.ClassPathResource("db/migration/V8__vehicle_gallery.sql"));
            assertThat(rows(db,"SELECT COUNT(*) FROM vehicle_images WHERE vehicle_id=900")).containsExactly("[0]");
            assertThat(rows(db,"SELECT COUNT(*) FROM vehicle_images WHERE vehicle_id=901")).containsExactly("[2]");
            st.executeUpdate("DELETE FROM vehicles_vehicle WHERE id=900");
        }
        assertThat(rows(db,"SELECT history_id,storage_key,position,CAST(is_primary AS UNSIGNED) FROM vehicle_image_history ORDER BY history_id")).isEqualTo(historyImages);
        assertThat(flyway(db).migrate().migrationsExecuted).isZero();
    }

    @Test void historicalMigrationSourcesAreImmutable() throws Exception {
        var expected=Map.ofEntries(
            Map.entry("V1__initial_deploy_schema.sql","c879a5bc14ec020fa8971c1ebc89589c18b87058b21e08d57fc24fc0d2322d7e"),
            Map.entry("V2__identity_user_profiles.sql","c82bc02dea548d93c127ddd565df1ca05fcd6eaf58f2b2b500becdcdd8196963"),
            Map.entry("V3__account_activation_invitations.sql","b2c5f25872d3e407401fc8efa1c1ccd0bf5fa5637c691973f5fcb7f8bd5a1887"),
            Map.entry("V4__membership_member_registry.sql","1f53bd3dfe2636613bc8c8907aada143a3cbcbbddda77c7eadbcb10951b3c6ce"),
            Map.entry("V5__principal_entity_lifecycle.sql","629cdcb878771a197a9d702561709e8c704f2018a7a034c49a447a1d5a9697d0"),
            Map.entry("V6__membership_onboarding_review_history.sql","2e38ea4b13a317d3d4752abc833e8d677557e963177201cb921061b64318b3a2"),
            Map.entry("V7__dependent_private_photo.sql","34d3f0bbc373e85f867162bddf30a92c21f32c7dc8a55e99c71874fc85fc1f3a"),
            Map.entry("V8__vehicle_gallery.sql","3d094e56e057f50e55fd15e8d9c2f08a2e94162dac5f1d7120247827371c4c07"));
        for(var entry:expected.entrySet()) {
            String text=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/migration",entry.getKey())).replace("\r\n","\n");
            var digest=java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertThat(java.util.HexFormat.of().formatHex(digest)).isEqualTo(entry.getValue());
        }
    }
    @Test void emptyDatabaseMatchesEveryJpaMappingAndIsIdempotent() throws Exception {
        String fresh = database();
        assertThat(flyway(fresh).migrate().migrationsExecuted).isEqualTo(migrationVersions().size());
        validateJpa(fresh);
        String expected = database();
        jpa(expected, Integer.MAX_VALUE, "create-only");
        registryPhysicalConstraints(expected);
        lifecyclePhysicalConstraints(expected);
        dependentPhotoPhysicalConstraints(expected);
        vehicleGalleryPhysicalConstraints(expected);
        eventGuestPhysicalConstraints(expected);
        assertThat(snapshot(fresh)).containsExactlyElementsOf(snapshot(expected));
        assertThat(flyway(fresh).migrate().migrationsExecuted).isZero();
        assertThat(count(fresh, "flyway_schema_history")).isEqualTo(migrationVersions().size());
    }

    @ParameterizedTest @ValueSource(ints = {1, 2, 3})
    void explicitBaselineOfVerifiedHistoricalSchemaConvergesWithoutLosingData(int version) throws Exception {
        String existing = historicalDatabase(version);
        try (var c = connection(existing); var statement = c.createStatement()) {
            statement.executeUpdate("insert into identity_users(id,name,cpf,status,created_at) "
                    + "values(700,'Migration fixture','00000000000','ACTIVE',UTC_TIMESTAMP(6))");
            statement.executeUpdate("insert into membership_applications(id,created_user_id,name,cpf,status,requested_at,version) "
                    + "values(800,700,'Synthetic existing application','00000000000','APPROVED',UTC_TIMESTAMP(6),0)");
            statement.executeUpdate("insert into authorization_permissions(id,code,module,description,created_at) "
                    + "values(901,'IDENTITY_USER_READ','IDENTITY','Synthetic existing permission',UTC_TIMESTAMP(6))");
        }
        var userColumnsBefore = fixtureColumns(existing, "identity_users");
        var usersBefore=fixtureRows(existing,"identity_users",userColumnsBefore);
        var applicationsBefore=fixtureRows(existing,"membership_applications");
        var permissionsBefore=fixtureRows(existing,"authorization_permissions");
        var baseline = Flyway.configure().dataSource(url(existing), MYSQL.getUsername(), MYSQL.getPassword())
                .baselineVersion(Integer.toString(version)).baselineDescription("Verified historical fixture")
                .baselineOnMigrate(false).cleanDisabled(true).load();
        baseline.baseline();
        assertThat(baseline.migrate().migrationsExecuted).isEqualTo(migrationVersions().size() - version);
        validateJpa(existing);
        String fresh = database();
        flyway(fresh).migrate();
        assertThat(snapshot(existing)).isEqualTo(snapshot(fresh));
        assertThat(count(existing, "identity_users")).isEqualTo(1);
        assertThat(fixtureRows(existing,"identity_users",userColumnsBefore)).isEqualTo(usersBefore);
        try (var c = connection(existing); var st = c.createStatement();
             var row = st.executeQuery("select archived_at from identity_users where id=700")) {
            assertThat(row.next()).isTrue();
            assertThat(row.getObject(1)).isNull();
        }
        assertThat(fixtureRows(existing,"membership_applications")).isEqualTo(applicationsBefore);
        assertThat(fixtureRows(existing,"authorization_permissions")).isEqualTo(permissionsBefore);
        assertThat(count(existing,"membership_members")).isZero();
        try(var c=connection(existing);var st=c.createStatement();var rows=st.executeQuery("select next_value from membership_registry_sequence where id=1")) {
            assertThat(rows.next()).isTrue();assertThat(rows.getObject(1)).isNull();
        }
        try (var c = connection(existing); var s = c.createStatement();
             var r = s.executeQuery("select name from identity_users where id=700")) {
            assertThat(r.next()).isTrue();
            assertThat(r.getString(1)).isEqualTo("Migration fixture");
        }
    }

    @Test void refusesNonEmptyUnversionedSchemaWithoutAutomaticBaseline() throws Exception {
        String existing = historicalDatabase(1);
        List<String> before = snapshot(existing);
        assertThatThrownBy(() -> flyway(existing).migrate()).isInstanceOf(FlywayException.class);
        assertThat(snapshot(existing)).isEqualTo(before);
    }

    @Test void hibernateRejectsMissingColumnInsteadOfRepairingIt() throws Exception {
        String db = database(); flyway(db).migrate();
        try (var c = connection(db); var s = c.createStatement()) {
            s.execute("alter table identity_user_profiles rename column street to legacy_street");
        }
        assertThatThrownBy(() -> validateJpa(db)).hasStackTraceContaining("missing column [street]");
        springContext(db).run(context -> assertThat(context).hasFailed().getFailure()
                .hasStackTraceContaining("missing column [street]"));
    }

    private static java.nio.file.Path experimentalMigrations(String sql) throws Exception {
        var dir = java.nio.file.Files.createTempDirectory("flyway-experiment-");
        try (var files = java.nio.file.Files.list(java.nio.file.Path.of("src/main/resources/db/migration"))) {
            for (var file : files.toList()) java.nio.file.Files.copy(file, dir.resolve(file.getFileName()));
        }
        if (sql != null) java.nio.file.Files.writeString(dir.resolve("V" + experimentalVersion() + "__lab_only.sql"), sql);
        return dir;
    }

    private static Flyway experimentalFlyway(String db, java.nio.file.Path dir) {
        return Flyway.configure().dataSource(url(db), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("filesystem:" + dir).baselineOnMigrate(false).cleanDisabled(true)
                .ignoreMigrationPatterns(new String[0]).load();
    }

    @Test void flywayRejectsChangedScriptWithoutEditingHistory() throws Exception {
        String db = database(); flyway(db).migrate();
        var dir = experimentalMigrations(null);
        java.nio.file.Files.writeString(dir.resolve("V2__identity_user_profiles.sql"), "-- changed checksum\n",
                java.nio.file.StandardOpenOption.APPEND);
        assertThatThrownBy(experimentalFlyway(db, dir)::validate).isInstanceOf(FlywayException.class);
        assertThat(flyway(db).getConfiguration().isCleanDisabled()).isTrue();
    }

    @Test void newMigrationRunsOnceAndOldApplicationRefusesFutureHistory() throws Exception {
        String db = database(); flyway(db).migrate();
        var dir = experimentalMigrations("CREATE TABLE migration_lab_probe(id BIGINT PRIMARY KEY); INSERT INTO migration_lab_probe VALUES(1);");
        var next = experimentalFlyway(db, dir);
        assertThat(next.migrate().migrationsExecuted).isEqualTo(1);
        next.validate(); validateJpa(db);
        assertThat(next.migrate().migrationsExecuted).isZero();
        assertThat(count(db, "migration_lab_probe")).isEqualTo(1);
        assertThat(count(db, "flyway_schema_history")).isEqualTo(migrationVersions().size() + 1);
        assertThatThrownBy(flyway(db)::validate).isInstanceOf(FlywayException.class);
        springContext(db).run(context -> assertThat(context).hasFailed().getFailure()
                .hasStackTraceContaining("Validate failed"));
    }

    @Test void invalidMigrationLeavesVisibleFailureAndDoesNotRepairHistory() throws Exception {
        String db = database(); flyway(db).migrate();
        var dir = experimentalMigrations("CREATE TABLE migration_lab_partial(id BIGINT PRIMARY KEY); INVALID SQL;");
        var next = experimentalFlyway(db, dir);
        assertThatThrownBy(next::migrate).isInstanceOf(FlywayException.class);
        assertThatThrownBy(next::validate).isInstanceOf(FlywayException.class);
        assertThat(count(db, "migration_lab_partial")).isZero();
        try (var c = connection(db); var s = c.createStatement();
             var r = s.executeQuery("select success from flyway_schema_history where version='" + experimentalVersion() + "'")) {
            assertThat(r.next()).isTrue(); assertThat(r.getBoolean(1)).isFalse();
        }
    }

    @Test void databaseEnforcesUniqueNotNullAndForeignKeyConstraints() throws Exception {
        String db = database(); flyway(db).migrate();
        try (var c = connection(db); var s = c.createStatement()) {
            assertThatThrownBy(() -> s.executeUpdate("insert into identity_users(name,cpf,status,created_at) "
                    + "values(null,'00000000000','ACTIVE',UTC_TIMESTAMP(6))")).isInstanceOf(java.sql.SQLException.class);
            s.executeUpdate("insert into identity_users(name,cpf,status,created_at) "
                    + "values('Migration fixture','00000000000','ACTIVE',UTC_TIMESTAMP(6))");
            assertThatThrownBy(() -> s.executeUpdate("insert into identity_users(name,cpf,status,created_at) "
                    + "values('Duplicate fixture','00000000000','ACTIVE',UTC_TIMESTAMP(6))"))
                    .isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> s.executeUpdate("insert into identity_user_profiles(user_id) values(999999)"))
                    .isInstanceOf(java.sql.SQLException.class);
        }
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    @org.springframework.boot.persistence.autoconfigure.EntityScan("com.jeepclub.backend")
    @org.springframework.context.annotation.Import(SchemaSafetyConfiguration.class)
    static class DatabaseContext { }

    @Test void springBootMigratesBeforeHibernateValidateUsingApplicationConfiguration() throws Exception {
        String db = database();
        springContext(db).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(jakarta.persistence.EntityManagerFactory.class).isOpen()).isTrue();
            assertThat(context.getBean(Flyway.class).info().current().getVersion().toString()).isEqualTo(latestVersion());
        });
        assertThat(count(db, "flyway_schema_history")).isEqualTo(migrationVersions().size());
    }

    @Test void unsafeOverrideIsRejectedBeforeFlywayCanWriteAnything() throws Exception {
        String db = database();
        springContext(db).withPropertyValues("spring.jpa.hibernate.ddl-auto=update").run(context ->
                assertThat(context).hasFailed().getFailure().hasStackTraceContaining("Unsafe database configuration"));
        try (var c = connection(db); var s = c.createStatement(); var r = s.executeQuery(
                "select count(*) from information_schema.tables where table_schema=database()")) {
            r.next(); assertThat(r.getInt(1)).isZero();
        }
    }

    private static org.springframework.boot.test.context.runner.ApplicationContextRunner springContext(String db) {
        return new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withInitializer(new org.springframework.boot.test.context.ConfigDataApplicationContextInitializer())
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class,
                        org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration.class,
                        org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration.class))
                .withUserConfiguration(DatabaseContext.class)
                .withPropertyValues("spring.datasource.url=" + url(db),
                        "spring.datasource.username=" + MYSQL.getUsername(),
                        "spring.datasource.password=" + MYSQL.getPassword());
    }

    @Test void readOnlyCollectorExportsCompleteMetadataWithoutChangingTheDatabase() throws Exception {
        String db = MYSQL.getDatabaseName(); flyway(db).migrate();
        try (var c = connection(db); var statement = c.createStatement()) {
            statement.executeUpdate("insert into identity_users(name,cpf,status,created_at) "
                    + "values('Collector fixture','00000000000','ACTIVE',UTC_TIMESTAMP(6))");
        }
        List<String> before = snapshot(db);
        var parent = java.nio.file.Path.of("target/audit-evidence");
        java.nio.file.Files.createDirectories(parent);
        var directory = parent.resolve("collector-" + UUID.randomUUID());
        var log = parent.resolve(directory.getFileName() + ".log");
        String bash = testBash();
        var builder = new ProcessBuilder(bash, "deploy/audit-schema.sh", directory.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("MYSQL_CONTAINER", MYSQL.getContainerId());
        builder.environment().put("BACKEND_CONTAINER", "no-backend-in-migration-fixture");
        var process = builder.start();
        try {
            assertThat(process.waitFor(90, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(process.exitValue()).withFailMessage(java.nio.file.Files.readString(log)).isZero();
        } finally { if (process.isAlive()) process.destroyForcibly(); }
        assertThat(snapshot(db)).isEqualTo(before);
        assertThat(count(db, "identity_users")).isEqualTo(1);
        try (var c = connection(db); var statement = c.createStatement(); var rows = statement.executeQuery(
                "select count(*) from information_schema.tables where table_schema=database() and table_name<>'flyway_schema_history'")) {
            rows.next();
            assertThat(java.nio.file.Files.readAllLines(directory.resolve("tables.tsv"))).hasSize(rows.getInt(1));
        }
        assertThat(java.nio.file.Files.readString(directory.resolve("flyway-history-present.txt")).trim()).isEqualTo("1");
        assertThat(java.nio.file.Files.readString(directory.resolve("schema.sql"))).contains("CREATE TABLE `identity_users`");
        assertThat(java.nio.file.Files.readString(directory.resolve("counts.tsv"))).contains("identity_users\t1");
        assertThat(java.nio.file.Files.readString(log)).doesNotContain(MYSQL.getPassword());
    }

    private static String testBash() {
        String configured=System.getProperty("test.bash");
        if(configured!=null && !configured.isBlank()) return configured;
        if(!System.getProperty("os.name").startsWith("Windows")) return "bash";
        var local=System.getenv("LOCALAPPDATA");
        var roots=List.of(java.nio.file.Path.of("C:/Program Files/Git"),
                java.nio.file.Path.of(local==null ? System.getProperty("user.home")+"/AppData/Local" : local,"Programs","Git"));
        for(var root:roots) {
            var candidate=root.resolve("bin/bash.exe");if(java.nio.file.Files.isRegularFile(candidate))return candidate.toString();
        }
        throw new IllegalStateException("Set -Dtest.bash to a Git Bash executable to run the collector test.");
    }

    private static List<org.flywaydb.core.api.MigrationVersion> migrationVersions() {
        try (var files = java.nio.file.Files.list(java.nio.file.Path.of("src/main/resources/db/migration"))) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("V") && name.endsWith(".sql") && name.contains("__"))
                    .map(name -> org.flywaydb.core.api.MigrationVersion.fromVersion(name.substring(1, name.indexOf("__"))))
                    .sorted().toList();
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
    private static String latestVersion() {
        var versions = migrationVersions();
        return versions.get(versions.size() - 1).toString();
    }
    private static String experimentalVersion() {
        return new java.math.BigInteger(latestVersion().split("\\.")[0]).add(java.math.BigInteger.ONE).toString();
    }

    private static Flyway flyway(String db) {
        return Flyway.configure().dataSource(url(db), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration").baselineOnMigrate(false)
                .cleanDisabled(true).ignoreMigrationPatterns(new String[0]).validateOnMigrate(true).validateMigrationNaming(true).outOfOrder(false).load();
    }
    private static String database() throws Exception {
        String name = "migration_" + UUID.randomUUID().toString().replace("-", "");
        try (var c = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = c.createStatement()) {
            statement.execute("create database " + name + " character set utf8mb4 collate utf8mb4_0900_ai_ci");
        }
        return name;
    }
    private static String historicalDatabase(int version) throws Exception {
        String name = database();
        // Frozen exports from the audited historical mappings; never regenerate from future entities.
        var resource = new org.springframework.core.io.ClassPathResource("database/historical/v" + version + ".sql");
        try (var c = connection(name)) {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(c, resource);
        }
        return name;
    }
    // Scalar cross-context keys and binary business-number collation are physical contracts,
    // deliberately not JPA associations. Apply them only to this JPA-generated comparison fixture.
    private static void registryPhysicalConstraints(String name) throws Exception {
        try (var c=connection(name);var statement=c.createStatement()) {
            statement.execute("ALTER TABLE authorization_permissions MODIFY COLUMN code ENUM ('AUTHENTICATION_EXPORT','AUTHENTICATION_PASSWORD_RECOVERY_CANCEL','AUTHENTICATION_PASSWORD_RECOVERY_READ','AUTHENTICATION_REFRESH_TOKEN_READ','AUTHENTICATION_REFRESH_TOKEN_REVOKE','AUTHENTICATION_SESSION_LOGOUT','AUTHENTICATION_SESSION_READ','AUTHENTICATION_USER_PASSWORD_RESET_LINK_GENERATE','AUTHENTICATION_USER_TEMPORARY_PASSWORD_GENERATE','AUTHORIZATION_EXPORT','AUTHORIZATION_PERMISSION_ASSIGN','AUTHORIZATION_PERMISSION_READ','AUTHORIZATION_PERMISSION_REVOKE','AUTHORIZATION_ROLE_CREATE','AUTHORIZATION_ROLE_DELETE','AUTHORIZATION_ROLE_DISABLE','AUTHORIZATION_ROLE_ENABLE','AUTHORIZATION_ROLE_READ','AUTHORIZATION_ROLE_UPDATE','AUTHORIZATION_USER_ROLE_ASSIGN','AUTHORIZATION_USER_ROLE_READ','AUTHORIZATION_USER_ROLE_REVOKE','BILLING_CHARGE_ASSIGNMENT_CREATE','BILLING_CHARGE_ASSIGNMENT_READ','BILLING_CHARGE_ASSIGNMENT_UPDATE','BILLING_CHARGE_CYCLE_ARCHIVE','BILLING_CHARGE_CYCLE_CANCEL','BILLING_CHARGE_CYCLE_FINISH','BILLING_CHARGE_CYCLE_GENERATE','BILLING_CHARGE_CYCLE_READ','BILLING_CHARGE_DEFINITION_CREATE','BILLING_CHARGE_DEFINITION_READ','BILLING_CHARGE_DEFINITION_UPDATE','BILLING_EXPORT','BILLING_MEMBER_CHARGE_CANCEL','BILLING_MEMBER_CHARGE_READ','BILLING_MEMBER_CHARGE_UPDATE','BILLING_PAYMENT_CONFIRM','BILLING_PAYMENT_READ','BILLING_PAYMENT_REJECT','BILLING_REFUND_APPROVE','BILLING_REFUND_CANCEL','BILLING_REFUND_EXPIRE','BILLING_REFUND_MARK_AS_REFUNDED','BILLING_REFUND_READ','BILLING_REFUND_REJECT','DEPENDENTS_DEPENDENT_EXPORT','DEPENDENTS_DEPENDENT_READ','HEALTH_MEDICAL_PROFILE_DELETE','HEALTH_MEDICAL_PROFILE_EXPORT','HEALTH_MEDICAL_PROFILE_READ','HEALTH_MEDICAL_PROFILE_UPDATE','IDENTITY_USER_CREATE','IDENTITY_USER_DISABLE','IDENTITY_USER_ENABLE','IDENTITY_USER_EXPORT','IDENTITY_USER_READ','IDENTITY_USER_UPDATE','MEMBERSHIP_BILLING_CONFIGURATION_READ','MEMBERSHIP_BILLING_CONFIGURATION_UPDATE','MEMBERSHIP_EXPORT','MEMBERSHIP_MEMBERSHIP_APPLICANT_BLOCK','MEMBERSHIP_MEMBERSHIP_APPLICANT_UNBLOCK','MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE','MEMBERSHIP_MEMBERSHIP_REQUEST_INVITE_RESEND','MEMBERSHIP_MEMBERSHIP_REQUEST_READ','MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT','PUBLICATIONS_COMMENT_CREATE','PUBLICATIONS_COMMENT_READ','PUBLICATIONS_EVENT_ADMIN_DASHBOARD_READ','PUBLICATIONS_EVENT_CANCEL','PUBLICATIONS_EVENT_CREATE','PUBLICATIONS_EVENT_DELETE','PUBLICATIONS_EVENT_FINISH','PUBLICATIONS_EVENT_GUEST_ADMIN_CREATE','PUBLICATIONS_EVENT_GUEST_ADMIN_READ','PUBLICATIONS_EVENT_GUEST_APPROVE','PUBLICATIONS_EVENT_GUEST_REJECT','PUBLICATIONS_EVENT_GUEST_REQUEST_CREATE','PUBLICATIONS_EVENT_GUEST_REQUEST_READ','PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ','PUBLICATIONS_EVENT_PUBLISH','PUBLICATIONS_EVENT_READ','PUBLICATIONS_EVENT_READ_ADMIN','PUBLICATIONS_EVENT_REGISTER','PUBLICATIONS_EVENT_REGISTRATION_CANCEL','PUBLICATIONS_EVENT_REGISTRATION_READ','PUBLICATIONS_EVENT_RIDE_OFFER_READ','PUBLICATIONS_EVENT_RIDE_OFFER_RESPOND','PUBLICATIONS_EVENT_RIDE_OFFER_SELECT','PUBLICATIONS_EVENT_UPDATE','PUBLICATIONS_EXPORT','PUBLICATIONS_FEED_READ','PUBLICATIONS_INTERACTION_LIKE','PUBLICATIONS_NOTICE_ARCHIVE','PUBLICATIONS_NOTICE_CREATE','PUBLICATIONS_NOTICE_DELETE','PUBLICATIONS_NOTICE_PUBLISH','PUBLICATIONS_NOTICE_READ','PUBLICATIONS_NOTICE_UPDATE','PUBLICATIONS_PUBLICATION_READ','PUBLICATIONS_SERVICE_ADMIN_DELETE','PUBLICATIONS_SERVICE_ADMIN_READ','PUBLICATIONS_SERVICE_CHANGE_REQUEST_ADMIN_READ','PUBLICATIONS_SERVICE_CHANGE_REQUEST_APPROVE','PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE','PUBLICATIONS_SERVICE_CHANGE_REQUEST_READ','PUBLICATIONS_SERVICE_CHANGE_REQUEST_REJECT','PUBLICATIONS_SERVICE_DELETE','PUBLICATIONS_SERVICE_READ','PUBLICATIONS_SERVICE_REQUEST_ADMIN_READ','PUBLICATIONS_SERVICE_REQUEST_APPROVE','PUBLICATIONS_SERVICE_REQUEST_CREATE','PUBLICATIONS_SERVICE_REQUEST_READ','PUBLICATIONS_SERVICE_REQUEST_REJECT','TOOLS_TOOL_ACTIVATE','TOOLS_TOOL_CREATE','TOOLS_TOOL_DEACTIVATE','TOOLS_TOOL_DELETE','TOOLS_TOOL_EXPORT','TOOLS_TOOL_READ','TOOLS_TOOL_UPDATE','VEHICLES_VEHICLE_CREATE','VEHICLES_VEHICLE_DELETE','VEHICLES_VEHICLE_EXPORT','VEHICLES_VEHICLE_READ','VEHICLES_VEHICLE_UPDATE','MEMBERSHIP_MEMBER_REGISTRATION_MANAGE','MEMBERSHIP_MEMBER_READ','MEMBERSHIP_MEMBER_AUDIT_READ','MEMBERSHIP_MEMBER_NUMBER_RESERVE','MEMBERSHIP_MEMBER_NUMBER_ISSUE','MEMBERSHIP_MEMBER_IMPORT') NOT NULL");
            statement.execute("alter table membership_members modify membership_number varchar(64) character set utf8mb4 collate utf8mb4_bin not null, add constraint fk_membership_member_identity foreign key (identity_user_id) references identity_users(id)");
            statement.execute("alter table membership_number_claims modify membership_number varchar(64) character set utf8mb4 collate utf8mb4_bin not null, add constraint fk_membership_claim_member foreign key (member_id) references membership_members(id)");
            statement.execute("alter table membership_member_audits modify previous_number varchar(64) character set utf8mb4 collate utf8mb4_bin null, modify new_number varchar(64) character set utf8mb4 collate utf8mb4_bin not null, add constraint fk_membership_audit_member foreign key (member_id) references membership_members(id)");
        }
    }
    private static void lifecyclePhysicalConstraints(String name) throws Exception {
        try (var c=connection(name); var st=c.createStatement()) {
            String oldType;
            try (var row=st.executeQuery("select column_type from information_schema.columns "
                    + "where table_schema=database() and table_name='authorization_permissions' and column_name='code'")) {
                assertThat(row.next()).isTrue(); oldType=row.getString(1);
            }
            st.execute("alter table authorization_permissions modify code " + oldType.substring(0,oldType.length()-1)
                    + ",'IDENTITY_USER_ARCHIVE','IDENTITY_USER_UNARCHIVE','IDENTITY_USER_DELETE','IDENTITY_USER_HISTORY_READ','AUTHORIZATION_ROLE_ARCHIVE','AUTHORIZATION_ROLE_UNARCHIVE','AUTHORIZATION_ROLE_HISTORY_READ','DEPENDENTS_DEPENDENT_ACTIVATE','DEPENDENTS_DEPENDENT_DEACTIVATE','DEPENDENTS_DEPENDENT_ARCHIVE','DEPENDENTS_DEPENDENT_UNARCHIVE','DEPENDENTS_DEPENDENT_DELETE','TOOLS_TOOL_ARCHIVE','TOOLS_TOOL_UNARCHIVE','VEHICLES_VEHICLE_ACTIVATE','VEHICLES_VEHICLE_DEACTIVATE','VEHICLES_VEHICLE_ARCHIVE','VEHICLES_VEHICLE_UNARCHIVE','BILLING_CHARGE_DEFINITION_DELETE','BILLING_CHARGE_DEFINITION_HISTORY_READ') not null");
            st.execute("alter table authorization_role_permission_deletion_history add constraint fk_role_permission_deletion_history foreign key (role_id) references authorization_role_deletion_history(original_id)");
            st.execute("alter table authorization_user_role_deletion_history add constraint fk_user_role_deletion_history foreign key (role_id) references authorization_role_deletion_history(original_id)");
        }
    }
    // Flyway-only binary collation and bounded position checks supplement the JPA fixture.
    private static void vehicleGalleryPhysicalConstraints(String name) throws Exception {
        try(var c=connection(name);var st=c.createStatement()) {
            for(var table:List.of("vehicle_images","vehicle_image_history"))
                st.execute("ALTER TABLE "+table+" MODIFY storage_key VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL");
            st.execute("ALTER TABLE vehicle_images ADD CONSTRAINT ck_vehicle_image_position CHECK(position BETWEEN 0 AND 4)");
            st.execute("ALTER TABLE vehicle_image_history ADD CONSTRAINT ck_vehicle_history_image_position CHECK(position BETWEEN 0 AND 4)");
        }
    }

    private static void eventGuestPhysicalConstraints(String name) throws Exception {
        try(var c=connection(name);var st=c.createStatement()) {
            st.execute("ALTER TABLE event_registrations ADD CONSTRAINT uk_event_registration_identity UNIQUE(id,event_id,user_id)");
            st.execute("ALTER TABLE event_guest_requests ADD INDEX idx_event_guest_cpf(event_id,cpf), ADD INDEX idx_event_guest_capacity(event_id,status,vehicle_id)");
            st.execute("ALTER TABLE event_ride_offers ADD INDEX idx_event_ride_response(guest_request_id,vehicle_id,status)");
            st.execute("ALTER TABLE event_guest_requests ADD CONSTRAINT fk_event_guest_origin FOREIGN KEY(origin_registration_id,event_id,requester_user_id) REFERENCES event_registrations(id,event_id,user_id)");
            st.execute("ALTER TABLE event_guest_requests ADD CONSTRAINT fk_event_guest_transport FOREIGN KEY(allocated_registration_id,event_id,responsible_user_id) REFERENCES event_registrations(id,event_id,user_id)");
            st.execute("ALTER TABLE event_guest_requests ADD CONSTRAINT fk_event_guest_selected_offer FOREIGN KEY(selected_offer_id) REFERENCES event_ride_offers(id)");
            st.execute("ALTER TABLE event_guest_requests ADD CONSTRAINT ck_event_guest_origin CHECK ((administrative=b'1' AND origin_registration_id IS NULL AND requested_vehicle_id IS NULL AND status<>'PENDING') OR (administrative=b'0' AND origin_registration_id IS NOT NULL AND requested_vehicle_id IS NOT NULL))");
            st.execute("ALTER TABLE event_guest_requests ADD CONSTRAINT ck_event_guest_transport CHECK ((status='APPROVED' AND vehicle_id IS NOT NULL AND allocated_registration_id IS NOT NULL AND responsible_user_id IS NOT NULL) OR (vehicle_id IS NULL AND allocated_registration_id IS NULL AND responsible_user_id IS NULL AND selected_offer_id IS NULL AND (status<>'APPROVED' OR administrative=b'1')))");
            st.execute("ALTER TABLE event_guest_requests ADD CONSTRAINT ck_event_guest_status CHECK(status IN ('PENDING','APPROVED','REJECTED','CANCELLED'))");
            st.execute("ALTER TABLE event_guest_requests ADD CONSTRAINT ck_event_guest_legacy_projection CHECK(approved_vehicle_id <=> IF(status='APPROVED',vehicle_id,NULL))");
            st.execute("ALTER TABLE event_ride_offers ADD CONSTRAINT ck_event_ride_withdrawal CHECK ((status='WITHDRAWN' AND withdrawn_at IS NOT NULL AND withdrawn_at>=COALESCE(selected_at,responded_at)) OR (status IN ('ACCEPTED','DECLINED','SELECTED') AND withdrawn_at IS NULL))");
            String old;
            try(var result=st.executeQuery("SELECT column_type FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='authorization_permissions' AND column_name='code'")) { assertThat(result.next()).isTrue();old=result.getString(1); }
            st.execute("ALTER TABLE authorization_permissions MODIFY code "+old.substring(0,old.length()-1)+",'PUBLICATIONS_EVENT_GUEST_REQUEST_CANCEL','PUBLICATIONS_EVENT_GUEST_ALLOCATE','PUBLICATIONS_EVENT_GUEST_UNASSIGN','PUBLICATIONS_EVENT_GUEST_CANCEL','PUBLICATIONS_EVENT_GUEST_REACTIVATE','PUBLICATIONS_EVENT_GUEST_HISTORY_READ') NOT NULL");
        }
    }

    @Test void guestLifecycleMigrationPreservesLegacyFactsAndReconcilesOriginsAndTransport() throws Exception {
        String db=database();
        Flyway.configure().dataSource(url(db),MYSQL.getUsername(),MYSQL.getPassword()).locations("classpath:db/migration").target("8").cleanDisabled(true).load().migrate();
        try(var c=connection(db);var st=c.createStatement()) {
            st.executeUpdate("INSERT INTO authorization_roles(id,name,kind,status,created_at) VALUES(990,'Synthetic CUSTOM','CUSTOM','ACTIVE','2026-10-01 12:00:00')");
            st.executeUpdate("INSERT INTO event_registrations(id,event_id,user_id,status,created_at,confirmed_at,version) VALUES(700,9000,800,'CONFIRMED','2026-10-01 12:00:00','2026-10-01 12:00:00',0),(701,9001,801,'CONFIRMED','2026-10-01 12:00:00','2026-10-01 12:00:00',0)");
            st.executeUpdate("INSERT INTO event_registration_vehicles(registration_id,vehicle_id) VALUES(700,111),(701,112)");
            st.executeUpdate("""
                INSERT INTO event_guest_requests(id,event_id,requester_user_id,vehicle_id,approved_vehicle_id,cpf,status,administrative,created_at,reviewer_id,reviewed_at,rejection_reason,guest_name,version) VALUES
                (1000,9000,800,111,NULL,'12345678901','PENDING',b'0','2026-10-01 12:00:00.123456',NULL,NULL,NULL,'Personal pending',0),
                (1001,9000,800,111,111,'12345678902','APPROVED',b'0','2026-10-01 12:00:00.123456',999,'2026-10-01 12:02:00.123456',NULL,'Personal approved',0),
                (1002,9000,800,111,NULL,'12345678903','REJECTED',b'0','2026-10-01 12:00:00.123456',999,'2026-10-01 12:02:00.123456','Original rejection','Personal rejected',0),
                (1003,9000,999,NULL,NULL,'12345678904','PENDING',b'1','2026-10-01 12:00:00.123456',NULL,NULL,NULL,'Admin pending',0),
                (1004,9001,999,112,112,'12345678905','APPROVED',b'1','2026-10-01 12:00:00.123456',998,'2026-10-01 12:02:00.123456',NULL,'Admin allocated',0),
                (1005,9001,999,NULL,NULL,'12345678906','REJECTED',b'1','2026-10-01 12:00:00.123456',997,'2026-10-01 12:02:00.123456','Original admin rejection','Admin rejected',0)
                """);
            st.executeUpdate("INSERT INTO event_ride_offers(id,event_id,guest_request_id,registration_id,user_id,vehicle_id,status,responded_at,selected_at,selected_guest_id,version) VALUES(2000,9001,1004,701,801,112,'SELECTED','2026-10-01 12:01:00.123456','2026-10-01 12:02:00.123456',1004,0),(2001,9000,1003,700,800,111,'ACCEPTED','2026-10-01 12:01:00.123456',NULL,NULL,0)");
        }
        var immutable=List.of("id","event_id","requester_user_id","cpf","administrative","created_at","reviewer_id","reviewed_at","rejection_reason","guest_name");
        var before=fixtureRows(db,"event_guest_requests",immutable);var offerColumns=fixtureColumns(db,"event_ride_offers");
        var offersBefore=fixtureRows(db,"event_ride_offers",offerColumns);
        flyway(db).migrate();validateJpa(db);
        assertThat(fixtureRows(db,"event_guest_requests",immutable)).isEqualTo(before);
        assertThat(fixtureRows(db,"event_ride_offers",offerColumns)).isEqualTo(offersBefore);
        assertThat(rows(db,"SELECT id,status,vehicle_id,origin_registration_id,requested_vehicle_id,allocated_registration_id,responsible_user_id,selected_offer_id FROM event_guest_requests ORDER BY id")).containsExactly(
            "[1000, PENDING, null, 700, 111, null, null, null]","[1001, APPROVED, 111, 700, 111, 700, 800, null]",
            "[1002, REJECTED, null, 700, 111, null, null, null]","[1003, APPROVED, null, null, null, null, null, null]",
            "[1004, APPROVED, 112, null, null, 701, 801, 2000]","[1005, REJECTED, null, null, null, null, null, null]");
        assertThat(rows(db,"SELECT previous_status,next_status,previous_vehicle_id,next_vehicle_id,actor_user_id FROM event_guest_transitions WHERE guest_request_id=1003")).containsExactly("[PENDING, APPROVED, null, null, null]");
        assertThat(rows(db,"SELECT previous_status,next_status FROM event_guest_transitions WHERE guest_request_id=1002")).containsExactly("[REJECTED, REJECTED]");
        assertThat(count(db,"event_guest_transitions")).isEqualTo(6);
        assertThat(rows(db,"SELECT COUNT(*) FROM authorization_role_permissions WHERE role_id=990")).containsExactly("[0]");
        try(var c=connection(db);var st=c.createStatement()) {
            st.executeUpdate("INSERT INTO event_guest_requests(event_id,requester_user_id,cpf,status,administrative,created_at,origin_registration_id,requested_vehicle_id) VALUES(9000,800,'12345678901','PENDING',b'0',UTC_TIMESTAMP(6),700,111)");
            st.executeUpdate("INSERT INTO event_guest_requests(event_id,requester_user_id,cpf,status,administrative,created_at,vehicle_id,approved_vehicle_id,allocated_registration_id,responsible_user_id) VALUES(9001,999,'12345678905','APPROVED',b'1',UTC_TIMESTAMP(6),112,112,701,801)");
            assertThatThrownBy(() -> st.executeUpdate("INSERT INTO event_guest_requests(event_id,requester_user_id,cpf,status,administrative,created_at,origin_registration_id,requested_vehicle_id) VALUES(9001,800,'12345678901','PENDING',b'0',UTC_TIMESTAMP(6),700,111)" )).isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> st.executeUpdate("UPDATE event_guest_requests SET status='CANCELLED' WHERE id=1004")).isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> st.executeUpdate("DELETE FROM event_registrations WHERE id=700")).isInstanceOf(java.sql.SQLException.class);
            st.executeUpdate("INSERT INTO authorization_permissions(code,module,description,created_at) VALUES('PUBLICATIONS_EVENT_GUEST_ALLOCATE','PUBLICATIONS','Synthetic new capability',UTC_TIMESTAMP(6))");
        }
        assertThat(flyway(db).migrate().migrationsExecuted).isZero();assertThat(count(db,"event_guest_transitions")).isEqualTo(6);
        assertThat(count(db,"event_guest_requests")).isEqualTo(8);
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void guestLifecyclePreflightRejectsUnresolvableLegacyWithoutChangingGuestData(boolean missingOrigin) throws Exception {
        String db=database();
        Flyway.configure().dataSource(url(db),MYSQL.getUsername(),MYSQL.getPassword()).locations("classpath:db/migration").target("8").cleanDisabled(true).load().migrate();
        try(var c=connection(db);var st=c.createStatement()) {
            st.executeUpdate("INSERT INTO event_guest_requests(id,event_id,requester_user_id,vehicle_id,approved_vehicle_id,cpf,status,administrative,created_at,version) VALUES(1000,9000,800,111,"+(missingOrigin?"NULL":"111")+",'12345678901','"+(missingOrigin?"PENDING":"APPROVED")+"',b'0',UTC_TIMESTAMP(6),0)");
            if(!missingOrigin) st.executeUpdate("INSERT INTO event_registrations(id,event_id,user_id,status,created_at,version) VALUES(700,9000,800,'CONFIRMED',UTC_TIMESTAMP(6),0)");
        }
        var before=fixtureRows(db,"event_guest_requests");
        assertThatThrownBy(() -> flyway(db).migrate()).isInstanceOf(FlywayException.class).hasStackTraceContaining("BACK-537:");
        assertThat(fixtureRows(db,"event_guest_requests")).isEqualTo(before);
        assertThat(fixtureColumns(db,"event_guest_requests")).doesNotContain("origin_registration_id");
        assertThat(rows(db,"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='event_guest_transitions'")).containsExactly("[0]");
    }

    private static void validateJpa(String name) throws Exception { jpa(name, Integer.MAX_VALUE, "validate"); }
    private static void dependentPhotoPhysicalConstraints(String name) throws Exception {
        try (var c=connection(name);var st=c.createStatement();var row=st.executeQuery("select column_type from information_schema.columns where table_schema=database() and table_name='authorization_permissions' and column_name='code'")) {
            assertThat(row.next()).isTrue();String old=row.getString(1);
            st.execute("alter table authorization_permissions modify code "+old.substring(0,old.length()-1)+",'DEPENDENTS_DEPENDENT_PHOTO_UPDATE') not null");
        }
    }

    @Test void dependentPhotoMigrationPreservesLiveAndHistoricalDataAndReplaysWithoutLosingReferences() throws Exception {
        String db=database();
        Flyway.configure().dataSource(url(db),MYSQL.getUsername(),MYSQL.getPassword()).locations("classpath:db/migration")
                .target("6").cleanDisabled(true).load().migrate();
        try(var c=connection(db);var st=c.createStatement()) {
            st.executeUpdate("INSERT INTO dependents_dependent(id,name,cpf,birth_date,relationship_type,user_id,status,created_at) VALUES(901,'Synthetic live photo fixture','00000000000','2015-01-01','CHILD',900,'ACTIVE',UTC_TIMESTAMP(6))");
            st.executeUpdate("INSERT INTO dependents_dependent_history(dependent_id,name,cpf,birth_date,relationship_type,user_id,status,deleted_by_user_id,created_at,deleted_at) VALUES(902,'Synthetic history photo fixture','00000000001','2015-01-01','CHILD',900,'INACTIVE',900,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6))");
        }
        var columns=fixtureColumns(db,"dependents_dependent");var before=fixtureRows(db,"dependents_dependent",columns);
        var historyColumns=fixtureColumns(db,"dependents_dependent_history");var historyBefore=fixtureRows(db,"dependents_dependent_history",historyColumns);
        flyway(db).migrate();validateJpa(db);
        assertThat(fixtureRows(db,"dependents_dependent",columns)).isEqualTo(before);
        assertThat(fixtureRows(db,"dependents_dependent_history",historyColumns)).isEqualTo(historyBefore);
        try(var c=connection(db);var st=c.createStatement()) {
            try(var row=st.executeQuery("SELECT photo_storage_key FROM dependents_dependent WHERE id=901")) {assertThat(row.next()).isTrue();assertThat(row.getString(1)).isNull();}
            st.executeUpdate("UPDATE dependents_dependent SET photo_storage_key='private-images/2026/10/19/550e8400-e29b-41d4-a716-446655440000.png' WHERE id=901");
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(c,new org.springframework.core.io.ClassPathResource("db/migration/V7__dependent_private_photo.sql"));
            try(var row=st.executeQuery("SELECT photo_storage_key FROM dependents_dependent WHERE id=901")) {assertThat(row.next()).isTrue();assertThat(row.getString(1)).startsWith("private-images/");}
        }
        assertThat(flyway(db).migrate().migrationsExecuted).isZero();
    }
    private static void jpa(String name, int version, String action) throws Exception {
        var registry = SchemaMappings.registry(Map.of(
                "hibernate.connection.url", url(name), "hibernate.connection.username", MYSQL.getUsername(),
                "hibernate.connection.password", MYSQL.getPassword(), "hibernate.default_catalog", name, "hibernate.hbm2ddl.auto", action));
        try (var ignored = SchemaMappings.metadata(registry, version).buildSessionFactory()) {
            // Building the factory performs the requested validation/isolated historical fixture creation.
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }
    private static String url(String name) {
        return "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306) + "/" + name
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }
    private static Connection connection(String name) throws Exception {
        return DriverManager.getConnection(url(name), MYSQL.getUsername(), MYSQL.getPassword());
    }
    private static List<String> fixtureColumns(String name, String table) throws Exception {
        var columns = new ArrayList<String>();
        try (var c = connection(name); var st = c.createStatement();
             var rows = st.executeQuery("select * from " + table + " where 1=0")) {
            for (int i=1; i<=rows.getMetaData().getColumnCount(); i++) columns.add(rows.getMetaData().getColumnName(i));
        }
        return columns;
    }
    private static List<String> fixtureRows(String name, String table, List<String> columns) throws Exception {
        var result = new ArrayList<String>();
        String selected = columns.stream().map(column -> String.valueOf((char)96) + column + (char)96).collect(java.util.stream.Collectors.joining(","));
        try (var c=connection(name); var st=c.createStatement();
             var rows=st.executeQuery("select " + selected + " from " + table + " order by id")) {
            while (rows.next()) {
                var cells=new ArrayList<String>();
                for(int i=1; i<=columns.size(); i++) cells.add(rows.getString(i));
                result.add(cells.toString());
            }
        }
        return result;
    }
    private static List<String> fixtureRows(String name,String table) throws Exception {
        var result=new ArrayList<String>();
        try(var c=connection(name);var st=c.createStatement();var rows=st.executeQuery("select * from "+table+" order by id")) {
            while(rows.next()) { var cells=new ArrayList<String>();for(int i=1;i<=rows.getMetaData().getColumnCount();i++)cells.add(rows.getString(i));result.add(cells.toString()); }
        }
        return result;
    }
    private static long count(String name, String table) throws Exception {
        try (var c = connection(name); var s = c.createStatement();
             var r = s.executeQuery("select count(*) from " + table)) { r.next(); return r.getLong(1); }
    }
    private static List<String> snapshot(String name) throws Exception {
        List<String> rows = new ArrayList<>();
        String[] queries = {
                "select TABLE_NAME,ENGINE,TABLE_COLLATION from information_schema.TABLES where TABLE_SCHEMA=? and TABLE_NAME<>'flyway_schema_history' order by TABLE_NAME",
                "select TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT,EXTRA,COLLATION_NAME,GENERATION_EXPRESSION from information_schema.COLUMNS where TABLE_SCHEMA=? and TABLE_NAME<>'flyway_schema_history' order by TABLE_NAME,COLUMN_NAME",
                "select TABLE_NAME,INDEX_NAME,NON_UNIQUE,SEQ_IN_INDEX,COLUMN_NAME,SUB_PART,INDEX_TYPE,EXPRESSION from information_schema.STATISTICS where TABLE_SCHEMA=? and TABLE_NAME<>'flyway_schema_history' order by TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX",
                "select TABLE_NAME,CONSTRAINT_NAME,CONSTRAINT_TYPE,ENFORCED from information_schema.TABLE_CONSTRAINTS where CONSTRAINT_SCHEMA=? and TABLE_NAME<>'flyway_schema_history' order by TABLE_NAME,CONSTRAINT_NAME",
                "select TABLE_NAME,CONSTRAINT_NAME,COLUMN_NAME,ORDINAL_POSITION,IF(REFERENCED_TABLE_SCHEMA=DATABASE(),'<current>',REFERENCED_TABLE_SCHEMA),REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME from information_schema.KEY_COLUMN_USAGE where CONSTRAINT_SCHEMA=? and TABLE_NAME<>'flyway_schema_history' order by TABLE_NAME,CONSTRAINT_NAME,ORDINAL_POSITION",
                "select TABLE_NAME,CONSTRAINT_NAME,UNIQUE_CONSTRAINT_NAME,MATCH_OPTION,UPDATE_RULE,DELETE_RULE from information_schema.REFERENTIAL_CONSTRAINTS where CONSTRAINT_SCHEMA=? order by TABLE_NAME,CONSTRAINT_NAME",
                "select CONSTRAINT_NAME,CHECK_CLAUSE from information_schema.CHECK_CONSTRAINTS where CONSTRAINT_SCHEMA=? order by CONSTRAINT_NAME"
        };
        try (var c = connection(name)) {
            for (int i=0;i<queries.length;i++) {
                try (var statement = c.prepareStatement(queries[i])) {
                    statement.setString(1,name);
                    try (var result = statement.executeQuery()) {
                        while(result.next()) {
                            var values = new ArrayList<String>();
                            for(int col=1; col<=result.getMetaData().getColumnCount(); col++) values.add(result.getString(col));
                            rows.add(i + ":" + values);
                        }
                    }
                }
            }
        }
        return rows;
    }

    @Test void lifecycleMigrationPreservesLegacyRowsAssociationsMediaAndRegistryAndCanBeReplayed() throws Exception {
        String db=database();
        Flyway.configure().dataSource(url(db),MYSQL.getUsername(),MYSQL.getPassword())
                .locations("classpath:db/migration").target("4").cleanDisabled(true).load().migrate();
        try(var c=connection(db);var st=c.createStatement()) {
            st.executeUpdate("insert into identity_users(id,name,cpf,status,created_at,disabled_at) values(700,'Synthetic legacy user','00000000000','DISABLED',UTC_TIMESTAMP(6)-interval 1 day,UTC_TIMESTAMP(6))");
            st.executeUpdate("insert into dependents_dependent(id,name,cpf,birth_date,relationship_type,user_id,status,created_at,updated_at) values(710,'Synthetic dependent','00000000001','2010-01-01','CHILD',700,'DISABLED',UTC_TIMESTAMP(6)-interval 1 day,UTC_TIMESTAMP(6))");
            st.executeUpdate("insert into dependents_dependent_history(id,dependent_id,name,cpf,birth_date,relationship_type,user_id,status,created_at,deleted_by_user_id,deleted_at) values(711,712,'Historical dependent','00000000002','2010-01-01','CHILD',700,'DISABLED',UTC_TIMESTAMP(6)-interval 1 day,700,UTC_TIMESTAMP(6))");
            st.executeUpdate("insert into tools(id,name,user_id,status,created_at) values(730,'Synthetic tool',700,'INACTIVE',UTC_TIMESTAMP(6)-interval 1 day)");
            st.executeUpdate("insert into vehicles_vehicle(id,nickname,photo,plate,renavam,brand,model,manufacturing_year,model_year,color,seating_capacity,fuel_type,engine_displacement,status,towing,owner_id,created_at) values(720,'Legacy photo','https://legacy.invalid/vehicle.png','LEG1A23','00000000003','Jeep','Test',2020,2020,'Blue',5,'DIESEL',2,'SOFT_DELETED',0,700,UTC_TIMESTAMP(6)-interval 1 day)");
            st.executeUpdate("insert into vehicles_vehicle(id,nickname,plate,renavam,brand,model,manufacturing_year,model_year,seating_capacity,fuel_type,engine_displacement,status,towing,owner_id,created_at) values(721,'Legacy null','NUL1A23','00000000004','Jeep','Test',2020,2020,5,'DIESEL',2,'SOFT_DELETED',0,700,UTC_TIMESTAMP(6)-interval 1 day)");
            st.executeUpdate("insert into vehicles_vehicle_history(id,vehicle_id,photo,plate,renavam,brand,model,manufacturing_year,model_year,seating_capacity,fuel_type,engine_displacement,status,towing,owner_id,created_at,deleted_by_user_id,deleted_at) values(724,725,'https://legacy.invalid/historical.png','HIS1A23','00000000005','Jeep','Test',2020,2020,5,'DIESEL',2,'SOFT_DELETED',0,700,UTC_TIMESTAMP(6)-interval 1 day,700,UTC_TIMESTAMP(6))");
            st.executeUpdate("insert into authorization_roles(id,name,kind,status,created_at,updated_at,deleted_at) values(750,'Legacy deleted','CUSTOM','DELETED',UTC_TIMESTAMP(6)-interval 1 day,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6)),(751,'Legacy inferred time','CUSTOM','DELETED',UTC_TIMESTAMP(6)-interval 1 day,UTC_TIMESTAMP(6),null)");
            st.executeUpdate("insert into authorization_permissions(id,code,module,description,created_at) values(760,'IDENTITY_USER_READ','IDENTITY','Synthetic permission',UTC_TIMESTAMP(6))");
            st.executeUpdate("insert into authorization_user_roles(id,user_id,role_id,created_at) values(770,700,750,UTC_TIMESTAMP(6)-interval 1 day)");
            st.executeUpdate("insert into authorization_role_permissions(id,role_id,permission_id,created_at) values(780,750,760,UTC_TIMESTAMP(6)-interval 1 day)");
            st.executeUpdate("insert into membership_members(id,identity_user_id,membership_number,created_at,updated_at,version) values(790,700,'512700',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),0)");
            var claimKey=new com.jeepclub.backend.memberships.core.domain.model.MembershipNumber("512700").claimKey();
            st.executeUpdate("insert into membership_number_claims(claim_key,membership_number,member_id,actor_user_id,claimed_at,reason) values('"+claimKey+"','512700',790,700,UTC_TIMESTAMP(6),'Synthetic migration fixture')");
            st.executeUpdate("insert into membership_member_audits(member_id,new_number,actor_user_id,occurred_at,reason,operation,outcome) values(790,'512700',700,UTC_TIMESTAMP(6),'Synthetic migration fixture','ASSIGN','SUCCESS')");
        }
        var unchanged=new java.util.LinkedHashMap<String,List<String>>();
        var columns=new java.util.LinkedHashMap<String,List<String>>();
        for(var table:List.of("identity_users","dependents_dependent","dependents_dependent_history","vehicles_vehicle","vehicles_vehicle_history","tools")) {
            var selected=fixtureColumns(db,table).stream().filter(column -> !column.equals("status")).toList();
            columns.put(table,selected);unchanged.put(table,fixtureRows(db,table,selected));
        }
        var registryBefore=rows(db,"select * from membership_members order by id");
        var claimsBefore=rows(db,"select * from membership_number_claims order by claim_key");
        var auditBefore=rows(db,"select * from membership_member_audits order by id");
        var permissionsBefore=rows(db,"select id,code,code+0,module,description,created_at,updated_at from authorization_permissions order by id");
        var processStatesBefore=rows(db,"select table_name,column_name,column_type from information_schema.columns where table_schema=database() and data_type='enum' and table_name not in ('identity_users','dependents_dependent','dependents_dependent_history','vehicles_vehicle','vehicles_vehicle_history','tools','tools_tool_history','authorization_roles','authorization_permissions') order by table_name,column_name");
        flyway(db).migrate();validateJpa(db);
        columns.forEach((table,selected) -> {
            try { assertThat(fixtureRows(db,table,selected)).isEqualTo(unchanged.get(table)); }
            catch(Exception e) { throw new RuntimeException(e); }
        });
        assertThat(rows(db,"select status from identity_users where id=700")).containsExactly("[INACTIVE]");
        assertThat(rows(db,"select status from dependents_dependent where id=710")).containsExactly("[INACTIVE]");
        assertThat(rows(db,"select status from dependents_dependent_history where id=711")).containsExactly("[INACTIVE]");
        assertThat(rows(db,"select status,legacy_deleted,archived_at is not null from vehicles_vehicle order by id")).containsExactly("[ARCHIVED, 1, 1]","[ARCHIVED, 1, 1]");
        assertThat(rows(db,"select status,legacy_deleted,archived_at is not null from vehicles_vehicle_history")).containsExactly("[ARCHIVED, 1, 1]");
        assertThat(count(db,"authorization_roles")).isZero();
        assertThat(count(db,"authorization_role_permissions")).isZero();
        assertThat(count(db,"authorization_user_roles")).isZero();
        assertThat(rows(db,"select original_id,previous_status,deleted_by_user_id,inferred_time,schema_version from authorization_role_deletion_history order by original_id")).containsExactly("[750, DELETED, null, 0, 1]","[751, DELETED, null, 1, 1]");
        assertThat(rows(db,"select original_id,role_id,permission_id,permission_code from authorization_role_permission_deletion_history")).containsExactly("[780, 750, 760, IDENTITY_USER_READ]");
        assertThat(rows(db,"select original_id,role_id,user_id from authorization_user_role_deletion_history")).containsExactly("[770, 750, 700]");
        assertThat(rows(db,"select * from membership_members order by id")).isEqualTo(registryBefore);
        assertThat(rows(db,"select * from membership_number_claims order by claim_key")).isEqualTo(claimsBefore);
        assertThat(rows(db,"select * from membership_member_audits where operation<>'LEGACY_CLAIM_SNAPSHOT' order by id")).isEqualTo(auditBefore);
        assertThat(rows(db,"select member_id,new_number,actor_user_id,reason,outcome from membership_member_audits where operation='LEGACY_CLAIM_SNAPSHOT'"))
                .containsExactly("[790, 512700, 700, Synthetic migration fixture, SUCCESS]");
        assertThat(rows(db,"select id,code,code+0,module,description,created_at,updated_at from authorization_permissions order by id")).isEqualTo(permissionsBefore);
        assertThat(rows(db,"select table_name,column_name,column_type from information_schema.columns where table_schema=database() and data_type='enum' and table_name not in ('identity_users','dependents_dependent','dependents_dependent_history','vehicles_vehicle','vehicles_vehicle_history','tools','tools_tool_history','authorization_roles','authorization_permissions') order by table_name,column_name")).isEqualTo(processStatesBefore);
        var historyBefore=rows(db,"select * from authorization_role_deletion_history order by original_id");
        try(var c=connection(db)) {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(c,new org.springframework.core.io.ClassPathResource("db/migration/V5__principal_entity_lifecycle.sql"));
        }
        assertThat(rows(db,"select * from authorization_role_deletion_history order by original_id")).isEqualTo(historyBefore);
        assertThat(count(db,"authorization_role_permission_deletion_history")).isEqualTo(1);
        assertThat(count(db,"authorization_user_role_deletion_history")).isEqualTo(1);
        try(var c=connection(db);var st=c.createStatement()) {
            assertThatThrownBy(() -> st.executeUpdate("delete from identity_users where id=700")).isInstanceOf(java.sql.SQLException.class);
        }
        assertThat(count(db,"identity_users")).isEqualTo(1);
        assertThat(rows(db,"select * from membership_members order by id")).isEqualTo(registryBefore);
        assertThat(flyway(db).migrate().migrationsExecuted).isZero();
    }

    @ParameterizedTest @ValueSource(strings={"ROOT","USER_TIMESTAMP"})
    void lifecyclePreflightRejectsContradictoryLegacyDataWithoutChangingOperationalRows(String invalid) throws Exception {
        String db=database();
        Flyway.configure().dataSource(url(db),MYSQL.getUsername(),MYSQL.getPassword())
                .locations("classpath:db/migration").target("4").cleanDisabled(true).load().migrate();
        try(var c=connection(db);var st=c.createStatement()) {
            if(invalid.equals("ROOT")) st.executeUpdate("insert into authorization_roles(id,name,kind,status,created_at,deleted_at) values(750,'ROOT','ROOT','DELETED',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6))");
            else st.executeUpdate("insert into identity_users(id,name,cpf,status,created_at) values(700,'Invalid timestamp fixture','00000000000','DISABLED',UTC_TIMESTAMP(6))");
        }
        var before=rows(db,invalid.equals("ROOT")?"select * from authorization_roles order by id":"select * from identity_users order by id");
        assertThatThrownBy(() -> flyway(db).migrate()).isInstanceOf(FlywayException.class)
                .hasStackTraceContaining("ck_back512_preflight");
        assertThat(rows(db,invalid.equals("ROOT")?"select * from authorization_roles order by id":"select * from identity_users order by id")).isEqualTo(before);
        assertThat(rows(db,"select count(*) from information_schema.columns where table_schema=database() and table_name='identity_users' and column_name='archived_at'")).containsExactly("[0]");
    }

    private static List<String> rows(String database,String query) throws Exception {
        var result=new ArrayList<String>();
        try(var c=connection(database);var st=c.createStatement();var rows=st.executeQuery(query)) {
            while(rows.next()) {
                var values=new ArrayList<String>();
                for(int i=1;i<=rows.getMetaData().getColumnCount();i++) {
                    Object value=rows.getObject(i);
                    if(value instanceof byte[] bytes && bytes.length==1) values.add(Integer.toString(bytes[0]&255));
                    else if(value instanceof Boolean flag) values.add(flag?"1":"0");
                    else values.add(rows.getString(i));
                }
                result.add(values.toString());
            }
        }
        return result;
    }
}
