package com.jeepclub.backend.storage.infra.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class StoragePreviewPermissionUpgradeMySqlIT {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.46");
    @Test void v24PreservesExistingPermissionsAndDoesNotGrantPreview() throws Exception {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword()).target("23").load().migrate();
        long readId,permissionCount,grantCount;
        try(var c=DriverManager.getConnection(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword());var s=c.createStatement()) {
            try(var r=s.executeQuery("select id from authorization_permissions where code='STORAGE_MEDIA_READ'")){assertThat(r.next()).isTrue();readId=r.getLong(1);}
            try(var r=s.executeQuery("select count(*) from authorization_permissions")){r.next();permissionCount=r.getLong(1);}
            try(var r=s.executeQuery("select count(*) from authorization_role_permissions")){r.next();grantCount=r.getLong(1);}
        }
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword()).load().migrate();
        try(var c=DriverManager.getConnection(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword());var s=c.createStatement()) {
            try(var r=s.executeQuery("select id from authorization_permissions where code='STORAGE_MEDIA_READ'")){assertThat(r.next()).isTrue();assertThat(r.getLong(1)).isEqualTo(readId);}
            try(var r=s.executeQuery("select count(*) from authorization_permissions")){r.next();assertThat(r.getLong(1)).isEqualTo(permissionCount+1);}
            try(var r=s.executeQuery("select count(*) from authorization_role_permissions")){r.next();assertThat(r.getLong(1)).isEqualTo(grantCount);}
            try(var r=s.executeQuery("select count(*) from authorization_role_permissions rp join authorization_permissions p on p.id=rp.permission_id where p.code='STORAGE_MEDIA_PREVIEW'")){r.next();assertThat(r.getLong(1)).isZero();}
            try(var r=s.executeQuery("select module,description from authorization_permissions where code='STORAGE_MEDIA_PREVIEW'")) {
                assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo("STORAGE");assertThat(r.getString(2)).isEqualTo("Storage: preview content");
            }
        }
    }
}
