package com.jeepclub.backend.storage.infra.persistence;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import org.flywaydb.core.Flyway;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class StorageDiscardUpgradeMySqlIT {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.46");
    @Test void v22PreservesPublishedDiscardTombstonesAndAudit() throws Exception {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword()).target("21").load().migrate();
        try(var connection=DriverManager.getConnection(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword());var sql=connection.createStatement()) {
            sql.executeUpdate("insert into storage_media_assets (id,origin,source_context,subject_type,purpose,classification,physical_state,created_at,updated_at,version) values (1,'LEGACY_IMPORT','UNKNOWN','UNKNOWN','UPGRADE_FIXTURE','UNCLASSIFIED','UNKNOWN',CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6),0)");
            sql.executeUpdate("insert into storage_media_control (media_id,received_sha256,state,retention_sensitive,legal_hold,version) values (1,repeat('a',64),'DISCARDED',true,true,7)");
            sql.executeUpdate("insert into storage_governance_events (media_id,operation,state,occurred_at) values (1,'LOGICAL_DISCARD','DISCARDED',CURRENT_TIMESTAMP(6))");
        }
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword()).load().migrate();
        try(var connection=DriverManager.getConnection(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword());var sql=connection.createStatement()) {
            try(var row=sql.executeQuery("select discarded,state,legal_hold,version from storage_media_control where media_id=1")) {
                assertThat(row.next()).isTrue();assertThat(row.getBoolean(1)).isTrue();assertThat(row.getString(2)).isEqualTo("VISIBLE");
                assertThat(row.getBoolean(3)).isTrue();assertThat(row.getLong(4)).isEqualTo(7);
            }
            try(var row=sql.executeQuery("select count(*) from storage_governance_events where media_id=1")) {assertThat(row.next()).isTrue();assertThat(row.getLong(1)).isEqualTo(1);}
        }
    }
}
