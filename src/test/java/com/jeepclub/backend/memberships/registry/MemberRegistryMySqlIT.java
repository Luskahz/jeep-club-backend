package com.jeepclub.backend.memberships.registry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"memberships.registry.imports.apply-enabled=true","memberships.registry.issuance.enabled=true","memberships.registry.issuance.approved=true","memberships.registry.issuance.initial-number=100","memberships.registry.issuance.width=3","memberships.registry.issuance.prefix=","spring.jpa.hibernate.ddl-auto=validate","spring.flyway.enabled=true","spring.sql.init.mode=never"})
@ActiveProfiles("test") @Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class MemberRegistryMySqlIT extends MemberRegistryScenarios {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.46").withUsername("root").withPassword("synthetic-test-only");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",MYSQL::getJdbcUrl);registry.add("spring.datasource.username",MYSQL::getUsername);
        registry.add("spring.datasource.password",MYSQL::getPassword);registry.add("spring.datasource.driver-class-name",() -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform",() -> "org.hibernate.dialect.MySQLDialect");
    }
    @Test void physicalForeignKeyPreventsSilentlyDeletingLinkedIdentity() {
        admin.assign(first,"FK-"+first,first,"Verified");
        var before=admin.byIdentity(first);long auditCount=audits.count(),claimCount=claims.count();
        assertThatThrownBy(() -> jdbc.update("delete from identity_users where id=?",first)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(admin.byIdentity(first)).isEqualTo(before);assertThat(audits.count()).isEqualTo(auditCount);assertThat(claims.count()).isEqualTo(claimCount);
    }
    @Test void allSixRegistryPermissionsArePersistedByNormalCatalogSynchronization() {
        assertThat(jdbc.queryForObject("select count(*) from authorization_permissions where code in ('MEMBERSHIP_MEMBER_READ','MEMBERSHIP_MEMBER_REGISTRATION_MANAGE','MEMBERSHIP_MEMBER_AUDIT_READ','MEMBERSHIP_MEMBER_NUMBER_RESERVE','MEMBERSHIP_MEMBER_NUMBER_ISSUE','MEMBERSHIP_MEMBER_IMPORT')",Long.class)).isEqualTo(6);
    }
}
