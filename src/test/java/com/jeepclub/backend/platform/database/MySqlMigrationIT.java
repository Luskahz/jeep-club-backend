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

    @Test void historicalMigrationSourcesAreImmutable() throws Exception {
        var expected=Map.ofEntries(
            Map.entry("V1__initial_deploy_schema.sql","c879a5bc14ec020fa8971c1ebc89589c18b87058b21e08d57fc24fc0d2322d7e"),
            Map.entry("V2__identity_user_profiles.sql","c82bc02dea548d93c127ddd565df1ca05fcd6eaf58f2b2b500becdcdd8196963"),
            Map.entry("V3__account_activation_invitations.sql","b2c5f25872d3e407401fc8efa1c1ccd0bf5fa5637c691973f5fcb7f8bd5a1887"));
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
        assertThat(snapshot(fresh)).isEqualTo(snapshot(expected));
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
        var usersBefore=fixtureRows(existing,"identity_users");
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
        assertThat(fixtureRows(existing,"identity_users")).isEqualTo(usersBefore);
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
            statement.execute("ALTER TABLE authorization_permissions MODIFY COLUMN code ENUM ('AUTHENTICATION_EXPORT','AUTHENTICATION_PASSWORD_RECOVERY_CANCEL','AUTHENTICATION_PASSWORD_RECOVERY_READ','AUTHENTICATION_REFRESH_TOKEN_READ','AUTHENTICATION_REFRESH_TOKEN_REVOKE','AUTHENTICATION_SESSION_LOGOUT','AUTHENTICATION_SESSION_READ','AUTHENTICATION_USER_PASSWORD_RESET_LINK_GENERATE','AUTHENTICATION_USER_TEMPORARY_PASSWORD_GENERATE','AUTHORIZATION_EXPORT','AUTHORIZATION_PERMISSION_ASSIGN','AUTHORIZATION_PERMISSION_READ','AUTHORIZATION_PERMISSION_REVOKE','AUTHORIZATION_ROLE_CREATE','AUTHORIZATION_ROLE_DELETE','AUTHORIZATION_ROLE_DISABLE','AUTHORIZATION_ROLE_ENABLE','AUTHORIZATION_ROLE_READ','AUTHORIZATION_ROLE_UPDATE','AUTHORIZATION_USER_ROLE_ASSIGN','AUTHORIZATION_USER_ROLE_READ','AUTHORIZATION_USER_ROLE_REVOKE','BILLING_CHARGE_ASSIGNMENT_CREATE','BILLING_CHARGE_ASSIGNMENT_READ','BILLING_CHARGE_ASSIGNMENT_UPDATE','BILLING_CHARGE_CYCLE_ARCHIVE','BILLING_CHARGE_CYCLE_CANCEL','BILLING_CHARGE_CYCLE_FINISH','BILLING_CHARGE_CYCLE_GENERATE','BILLING_CHARGE_CYCLE_READ','BILLING_CHARGE_DEFINITION_CREATE','BILLING_CHARGE_DEFINITION_READ','BILLING_CHARGE_DEFINITION_UPDATE','BILLING_EXPORT','BILLING_MEMBER_CHARGE_CANCEL','BILLING_MEMBER_CHARGE_READ','BILLING_MEMBER_CHARGE_UPDATE','BILLING_PAYMENT_CONFIRM','BILLING_PAYMENT_READ','BILLING_PAYMENT_REJECT','BILLING_REFUND_APPROVE','BILLING_REFUND_CANCEL','BILLING_REFUND_EXPIRE','BILLING_REFUND_MARK_AS_REFUNDED','BILLING_REFUND_READ','BILLING_REFUND_REJECT','DEPENDENTS_DEPENDENT_EXPORT','DEPENDENTS_DEPENDENT_READ','HEALTH_MEDICAL_PROFILE_DELETE','HEALTH_MEDICAL_PROFILE_EXPORT','HEALTH_MEDICAL_PROFILE_READ','HEALTH_MEDICAL_PROFILE_UPDATE','IDENTITY_USER_CREATE','IDENTITY_USER_DISABLE','IDENTITY_USER_ENABLE','IDENTITY_USER_EXPORT','IDENTITY_USER_READ','IDENTITY_USER_UPDATE','MEMBERSHIP_BILLING_CONFIGURATION_READ','MEMBERSHIP_BILLING_CONFIGURATION_UPDATE','MEMBERSHIP_EXPORT','MEMBERSHIP_MEMBERSHIP_APPLICANT_BLOCK','MEMBERSHIP_MEMBERSHIP_APPLICANT_UNBLOCK','MEMBERSHIP_MEMBERSHIP_REQUEST_APPROVE','MEMBERSHIP_MEMBERSHIP_REQUEST_INVITE_RESEND','MEMBERSHIP_MEMBERSHIP_REQUEST_READ','MEMBERSHIP_MEMBERSHIP_REQUEST_REJECT','PUBLICATIONS_COMMENT_CREATE','PUBLICATIONS_COMMENT_READ','PUBLICATIONS_EVENT_ADMIN_DASHBOARD_READ','PUBLICATIONS_EVENT_CANCEL','PUBLICATIONS_EVENT_CREATE','PUBLICATIONS_EVENT_DELETE','PUBLICATIONS_EVENT_FINISH','PUBLICATIONS_EVENT_GUEST_ADMIN_CREATE','PUBLICATIONS_EVENT_GUEST_ADMIN_READ','PUBLICATIONS_EVENT_GUEST_APPROVE','PUBLICATIONS_EVENT_GUEST_REJECT','PUBLICATIONS_EVENT_GUEST_REQUEST_CREATE','PUBLICATIONS_EVENT_GUEST_REQUEST_READ','PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ','PUBLICATIONS_EVENT_PUBLISH','PUBLICATIONS_EVENT_READ','PUBLICATIONS_EVENT_READ_ADMIN','PUBLICATIONS_EVENT_REGISTER','PUBLICATIONS_EVENT_REGISTRATION_CANCEL','PUBLICATIONS_EVENT_REGISTRATION_READ','PUBLICATIONS_EVENT_RIDE_OFFER_READ','PUBLICATIONS_EVENT_RIDE_OFFER_RESPOND','PUBLICATIONS_EVENT_RIDE_OFFER_SELECT','PUBLICATIONS_EVENT_UPDATE','PUBLICATIONS_EXPORT','PUBLICATIONS_FEED_READ','PUBLICATIONS_INTERACTION_LIKE','PUBLICATIONS_NOTICE_ARCHIVE','PUBLICATIONS_NOTICE_CREATE','PUBLICATIONS_NOTICE_DELETE','PUBLICATIONS_NOTICE_PUBLISH','PUBLICATIONS_NOTICE_READ','PUBLICATIONS_NOTICE_UPDATE','PUBLICATIONS_PUBLICATION_READ','PUBLICATIONS_SERVICE_ADMIN_DELETE','PUBLICATIONS_SERVICE_ADMIN_READ','PUBLICATIONS_SERVICE_CHANGE_REQUEST_ADMIN_READ','PUBLICATIONS_SERVICE_CHANGE_REQUEST_APPROVE','PUBLICATIONS_SERVICE_CHANGE_REQUEST_CREATE','PUBLICATIONS_SERVICE_CHANGE_REQUEST_READ','PUBLICATIONS_SERVICE_CHANGE_REQUEST_REJECT','PUBLICATIONS_SERVICE_DELETE','PUBLICATIONS_SERVICE_READ','PUBLICATIONS_SERVICE_REQUEST_ADMIN_READ','PUBLICATIONS_SERVICE_REQUEST_APPROVE','PUBLICATIONS_SERVICE_REQUEST_CREATE','PUBLICATIONS_SERVICE_REQUEST_READ','PUBLICATIONS_SERVICE_REQUEST_REJECT','TOOLS_TOOL_ACTIVATE','TOOLS_TOOL_CREATE','TOOLS_TOOL_DEACTIVATE','TOOLS_TOOL_DELETE','TOOLS_TOOL_EXPORT','TOOLS_TOOL_READ','TOOLS_TOOL_UPDATE','VEHICLES_VEHICLE_CREATE','VEHICLES_VEHICLE_DELETE','VEHICLES_VEHICLE_EXPORT','VEHICLES_VEHICLE_READ','VEHICLES_VEHICLE_UPDATE','MEMBERSHIP_MEMBER_REGISTRATION_MANAGE') NOT NULL");
            statement.execute("alter table membership_members modify membership_number varchar(64) character set utf8mb4 collate utf8mb4_bin not null, add constraint fk_membership_member_identity foreign key (identity_user_id) references identity_users(id)");
            statement.execute("alter table membership_number_claims modify membership_number varchar(64) character set utf8mb4 collate utf8mb4_bin not null, add constraint fk_membership_claim_member foreign key (member_id) references membership_members(id)");
            statement.execute("alter table membership_member_audits modify previous_number varchar(64) character set utf8mb4 collate utf8mb4_bin null, modify new_number varchar(64) character set utf8mb4 collate utf8mb4_bin not null, add constraint fk_membership_audit_member foreign key (member_id) references membership_members(id)");
        }
    }
    private static void validateJpa(String name) throws Exception { jpa(name, Integer.MAX_VALUE, "validate"); }
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
}
