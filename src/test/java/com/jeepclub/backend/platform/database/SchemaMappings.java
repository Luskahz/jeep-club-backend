package com.jeepclub.backend.platform.database;

import jakarta.persistence.Entity;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import java.util.Map;

/** Complete application mappings, including abstract inheritance roots. */
final class SchemaMappings {
    static StandardServiceRegistry registry(Map<String, Object> settings) {
        return new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .applySetting("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl")
                .applySetting("hibernate.implicit_naming_strategy", "org.springframework.boot.hibernate.SpringImplicitNamingStrategy")
                .applySettings(settings).build();
    }
    static Metadata metadata(StandardServiceRegistry registry) throws Exception {
        return metadata(registry, 3);
    }
    static Metadata metadata(StandardServiceRegistry registry, int version) throws Exception {
        var sources = new MetadataSources(registry);
        var readers = new CachingMetadataReaderFactory();
        for (var resource : new PathMatchingResourcePatternResolver()
                .getResources("classpath*:com/jeepclub/backend/**/*.class")) {
            var reader = readers.getMetadataReader(resource);
            if (reader.getAnnotationMetadata().hasAnnotation(Entity.class.getName())) {
                String name = reader.getClassMetadata().getClassName();
                if (version < 2 && name.endsWith(".UserProfileEntity")) continue;
                if (version < 3 && name.endsWith(".AccountActivationInvitationEntity")) continue;
                sources.addAnnotatedClass(Class.forName(name));
            }
        }
        return sources.buildMetadata();
    }
    private SchemaMappings() { }
}
