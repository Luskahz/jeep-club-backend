package com.jeepclub.backend.storage.infra.persistence;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@org.junit.jupiter.api.parallel.Isolated
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="storage.admin.timezoneDiagnostic",matches="true")
class StorageTimezoneMySqlIT {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.46");
    @org.junit.jupiter.api.BeforeAll static void migrate() {
        org.flywaydb.core.Flyway.configure().dataSource(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword()).load().migrate();
    }
    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named="storage.admin.timezoneDiagnostic",matches="true")
    void diagnosticJpaTimestampAcrossJvmDefaultTimezones() {
        var original=java.util.TimeZone.getDefault();var instant=java.time.Instant.parse("2026-11-13T12:00:00Z");
        try {
            var baseline=timezoneRoundTrip(MYSQL.getJdbcUrl(),false,instant);
            var url=MYSQL.getJdbcUrl()+(MYSQL.getJdbcUrl().contains("?")?"&":"?")+"serverTimezone=UTC";
            var configuredUrl=timezoneRoundTrip(url,false,instant);var configuredHibernate=timezoneRoundTrip(MYSQL.getJdbcUrl(),true,instant);
            System.out.println("CROSS_TIMEZONE_DIAGNOSTIC written="+instant+" baseline="+baseline+" serverTimezoneUTC="+configuredUrl+" hibernateJdbcUTC="+configuredHibernate);
            org.assertj.core.api.Assertions.assertThat(configuredUrl).isEqualTo(instant);
            org.assertj.core.api.Assertions.assertThat(configuredHibernate).isEqualTo(instant);
        } finally {java.util.TimeZone.setDefault(original);}
    }
    private java.time.Instant timezoneRoundTrip(String url,boolean jdbcUtc,java.time.Instant instant) {
        Long id;java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Sao_Paulo"));
        var registry=timezoneRegistry(url,jdbcUtc);
        try(var factory=new org.hibernate.boot.MetadataSources(registry).addAnnotatedClass(com.jeepclub.backend.storage.infra.persistence.entity.MediaAssetEntity.class).buildMetadata().buildSessionFactory();var session=factory.openSession()) {
            var tx=session.beginTransaction();var e=new com.jeepclub.backend.storage.infra.persistence.entity.MediaAssetEntity();
            e.setOrigin(com.jeepclub.backend.storage.core.domain.enums.MediaOrigin.UPLOAD);e.setSourceContext(com.jeepclub.backend.storage.core.domain.enums.MediaContext.IDENTITY_PROFILE);
            e.setUploadedByUserId(990345L);e.setOwnerUserId(990345L);e.setSubjectType(com.jeepclub.backend.storage.core.domain.enums.MediaSubjectType.USER);e.setSubjectId(990345L);
            e.setPurpose("PHOTO");e.setClassification(com.jeepclub.backend.storage.core.domain.enums.MediaClassification.CLASSIFIED);e.setPhysicalState(com.jeepclub.backend.storage.core.domain.enums.MediaPhysicalState.PRESENT);
            e.setCreatedAt(instant);e.setUpdatedAt(instant);session.persist(e);tx.commit();id=e.getMediaId();
        } finally {org.hibernate.boot.registry.StandardServiceRegistryBuilder.destroy(registry);}
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));var second=timezoneRegistry(url,jdbcUtc);
        try(var factory=new org.hibernate.boot.MetadataSources(second).addAnnotatedClass(com.jeepclub.backend.storage.infra.persistence.entity.MediaAssetEntity.class).buildMetadata().buildSessionFactory();var session=factory.openSession()) {
            return session.find(com.jeepclub.backend.storage.infra.persistence.entity.MediaAssetEntity.class,id).getCreatedAt();
        } finally {org.hibernate.boot.registry.StandardServiceRegistryBuilder.destroy(second);}
    }
    private org.hibernate.boot.registry.StandardServiceRegistry timezoneRegistry(String url,boolean jdbcUtc) {
        var builder=new org.hibernate.boot.registry.StandardServiceRegistryBuilder().applySettings(java.util.Map.of(
            "hibernate.connection.driver_class","com.mysql.cj.jdbc.Driver","hibernate.connection.url",url,
            "hibernate.connection.username",MYSQL.getUsername(),"hibernate.connection.password",MYSQL.getPassword(),
            "hibernate.dialect","org.hibernate.dialect.MySQLDialect","hibernate.hbm2ddl.auto","none"));
        if(jdbcUtc) builder.applySetting("hibernate.jdbc.time_zone","UTC");return builder.build();
    }
}
