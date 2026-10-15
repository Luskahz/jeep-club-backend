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
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.44")
            .withUsername("root").withPassword("migration-test-only")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci");

    @Test void emptyDatabaseMatchesEveryJpaMappingAndIsIdempotent() throws Exception {
        String fresh = database();
        assertThat(flyway(fresh, "3").migrate().migrationsExecuted).isEqualTo(3);
        validateJpa(fresh);
        String expected = historicalDatabase(3);
        assertThat(snapshot(fresh)).isEqualTo(snapshot(expected));
        assertThat(flyway(fresh, "3").migrate().migrationsExecuted).isZero();
        assertThat(count(fresh, "flyway_schema_history")).isEqualTo(3);
    }

    @ParameterizedTest @ValueSource(ints = {1, 2, 3})
    void explicitBaselineOfVerifiedHistoricalSchemaConvergesWithoutLosingData(int version) throws Exception {
        String existing = historicalDatabase(version);
        try (var c = connection(existing); var statement = c.createStatement()) {
            statement.executeUpdate("insert into identity_users(id,name,cpf,status,created_at) "
                    + "values(700,'Migration fixture','00000000000','ACTIVE',UTC_TIMESTAMP(6))");
        }
        var baseline = Flyway.configure().dataSource(url(existing), MYSQL.getUsername(), MYSQL.getPassword())
                .baselineVersion(Integer.toString(version)).baselineDescription("Verified historical fixture")
                .baselineOnMigrate(false).cleanDisabled(true).load();
        baseline.baseline();
        assertThat(baseline.migrate().migrationsExecuted).isEqualTo(3 - version);
        validateJpa(existing);
        String fresh = database();
        flyway(fresh, "3").migrate();
        assertThat(snapshot(existing)).isEqualTo(snapshot(fresh));
        assertThat(count(existing, "identity_users")).isEqualTo(1);
        try (var c = connection(existing); var s = c.createStatement();
             var r = s.executeQuery("select name from identity_users where id=700")) {
            assertThat(r.next()).isTrue();
            assertThat(r.getString(1)).isEqualTo("Migration fixture");
        }
    }

    @Test void refusesNonEmptyUnversionedSchemaWithoutAutomaticBaseline() throws Exception {
        String existing = historicalDatabase(1);
        List<String> before = snapshot(existing);
        assertThatThrownBy(() -> flyway(existing, "3").migrate()).isInstanceOf(FlywayException.class);
        assertThat(snapshot(existing)).isEqualTo(before);
    }

    @Test void hibernateRejectsMissingColumnInsteadOfRepairingIt() throws Exception {
        String db = database(); flyway(db, "3").migrate();
        try (var c = connection(db); var s = c.createStatement()) {
            s.execute("alter table identity_user_profiles drop column street");
        }
        assertThatThrownBy(() -> validateJpa(db)).hasStackTraceContaining("missing column [street]");
        springContext(db).run(context -> assertThat(context).hasFailed().getFailure()
                .hasStackTraceContaining("missing column [street]"));
    }

    @Test void flywayRejectsAlteredChecksumAndCleanIsDisabled() throws Exception {
        String db = database(); var flyway = flyway(db, "3"); flyway.migrate();
        try (var c = connection(db); var s = c.createStatement()) {
            s.executeUpdate("update flyway_schema_history set checksum=0 where version='2'");
        }
        assertThatThrownBy(flyway::validate).isInstanceOf(FlywayException.class);
        assertThatThrownBy(flyway::clean).isInstanceOf(FlywayException.class);
        assertThat(count(db, "identity_users")).isZero();
    }

    @Test void refusesSchemaHistoryFromANewerApplicationVersion() throws Exception {
        String db = database(); var flyway = flyway(db, "3"); flyway.migrate();
        try (var c = connection(db); var s = c.createStatement()) {
            s.executeUpdate("insert into flyway_schema_history(installed_rank,version,description,type,script,checksum,installed_by,execution_time,success) "
                    + "values(4,'4','Future fixture','SQL','V4__future_fixture.sql',123,'fixture',0,1)");
        }
        assertThatThrownBy(flyway::validate).isInstanceOf(FlywayException.class);
        springContext(db).run(context -> assertThat(context).hasFailed().getFailure()
                .hasStackTraceContaining("Validate failed"));
    }

    @Test void databaseEnforcesUniqueNotNullAndForeignKeyConstraints() throws Exception {
        String db = database(); flyway(db, "3").migrate();
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

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @org.springframework.boot.persistence.autoconfigure.EntityScan("com.jeepclub.backend")
    @org.springframework.context.annotation.Import(SchemaSafetyConfiguration.class)
    static class DatabaseContext { }

    @Test void springBootMigratesBeforeHibernateValidateUsingApplicationConfiguration() throws Exception {
        String db = database();
        springContext(db).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(jakarta.persistence.EntityManagerFactory.class).isOpen()).isTrue();
            assertThat(context.getBean(Flyway.class).info().current().getVersion().toString()).isEqualTo("3");
        });
        assertThat(count(db, "flyway_schema_history")).isEqualTo(3);
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
        String db = MYSQL.getDatabaseName(); flyway(db, "3").migrate();
        try (var c = connection(db); var statement = c.createStatement()) {
            statement.executeUpdate("insert into identity_users(name,cpf,status,created_at) "
                    + "values('Collector fixture','00000000000','ACTIVE',UTC_TIMESTAMP(6))");
        }
        List<String> before = snapshot(db);
        var parent = java.nio.file.Path.of("target/audit-evidence");
        java.nio.file.Files.createDirectories(parent);
        var directory = parent.resolve("collector-" + UUID.randomUUID());
        var log = parent.resolve(directory.getFileName() + ".log");
        String bash = System.getProperty("os.name").startsWith("Windows")
                ? "C:/Program Files/Git/bin/bash.exe" : "bash";
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
        assertThat(java.nio.file.Files.readAllLines(directory.resolve("tables.tsv"))).hasSize(60);
        assertThat(java.nio.file.Files.readString(directory.resolve("flyway-history-present.txt")).trim()).isEqualTo("1");
        assertThat(java.nio.file.Files.readString(directory.resolve("schema.sql"))).contains("CREATE TABLE `identity_users`");
        assertThat(java.nio.file.Files.readString(directory.resolve("counts.tsv"))).contains("identity_users\t1");
        assertThat(java.nio.file.Files.readString(log)).doesNotContain(MYSQL.getPassword());
    }

    private static Flyway flyway(String db, String target) {
        return Flyway.configure().dataSource(url(db), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration").target(target).baselineOnMigrate(false)
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
        jpa(name, version, "create-only");
        return name;
    }
    private static void validateJpa(String name) throws Exception { jpa(name, 3, "validate"); }
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
