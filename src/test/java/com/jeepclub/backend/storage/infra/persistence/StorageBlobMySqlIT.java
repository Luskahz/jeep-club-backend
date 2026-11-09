package com.jeepclub.backend.storage.infra.persistence;

import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Testcontainers
class StorageBlobMySqlIT extends StorageUploadFlowTest {
    @org.junit.jupiter.api.Test void uppercaseConstraintIsEnforcedAfterFlywayEvolution() throws Exception {
        var a=upload.upload(88,com.jeepclub.backend.storage.core.domain.enums.MediaContext.IDENTITY_PROFILE,"88",image(88));
        var v=catalog.findVariant(a.mediaId(),"AVATAR_SMALL").orElseThrow();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("update storage_media_variants set variant_code='avatar_small' where id=?",v.variantId()))
            .isInstanceOf(RuntimeException.class);
    }
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.46");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);
        r.add("spring.datasource.password",MYSQL::getPassword);r.add("spring.datasource.driver-class-name",() -> "com.mysql.cj.jdbc.Driver");
        r.add("spring.jpa.database-platform",() -> "org.hibernate.dialect.MySQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto",() -> "validate");r.add("spring.flyway.enabled",() -> true);
    }
}
