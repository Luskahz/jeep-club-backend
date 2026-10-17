package com.jeepclub.backend.platform.database;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class SchemaMappingExportTest {
    @Test
    void exportsCompleteMysqlReferenceWithoutConnectingToAnyDatabase() throws Exception {
        var target = Path.of("target/audit-evidence/develop-hibernate-mysql.sql");
        Files.createDirectories(target.getParent());
        Files.deleteIfExists(target);
        var registry = SchemaMappings.registry(Map.of(
                "jakarta.persistence.schema-generation.database.action", "none",
                "jakarta.persistence.schema-generation.scripts.action", "create",
                "jakarta.persistence.schema-generation.scripts.create-target", target.toString(),
                "hibernate.hbm2ddl.delimiter", ";"));
        try (var factory = SchemaMappings.metadata(registry).buildSessionFactory()) {
            assertThat(factory.getMetamodel().getEntities()).isNotEmpty();
            assertThat(Files.readString(target)).contains("create table identity_users");
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
