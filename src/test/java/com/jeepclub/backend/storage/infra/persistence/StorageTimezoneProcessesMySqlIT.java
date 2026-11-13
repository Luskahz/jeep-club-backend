package com.jeepclub.backend.storage.infra.persistence;

import com.jeepclub.backend.storage.core.domain.enums.*;
import com.jeepclub.backend.storage.infra.persistence.entity.MediaAssetEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers @Isolated
@EnabledIfSystemProperty(named="storage.admin.timezoneProcesses",matches="true")
class StorageTimezoneProcessesMySqlIT {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.46");
    @BeforeAll static void migrate() {
        org.flywaydb.core.Flyway.configure().dataSource(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword()).load().migrate();
    }
    @Test void observesIndependentJvmTimezoneAndConfiguration(@TempDir Path output) throws Exception {
        String expected="2026-11-13T12:00:00Z";
        String oldId=probe(output,"America/Sao_Paulo","write",false,"0");
        String baseline=probe(output,"UTC","read",false,oldId);
        String changed=probe(output,"America/Sao_Paulo","read",true,oldId);
        String baselineFilter=probe(output,"UTC","query",false,oldId);
        String utcId=probe(output,"America/Sao_Paulo","write",true,"0");
        String configured=probe(output,"UTC","read",true,utcId);
        String configuredFilter=probe(output,"UTC","query",true,utcId);
        System.out.println("PROCESS_TIMEZONE_DIAGNOSTIC written="+expected+" baseline="+baseline+" changed="+changed+" configured="+configured+" baselineFilter="+baselineFilter+" configuredFilter="+configuredFilter);
        assertThat(configured).isEqualTo(expected);
        assertThat(configuredFilter).isEqualTo("1");
    }
    private String probe(Path output,String zone,String operation,boolean utc,String id) throws Exception {
        Path log=Files.createTempFile(output,"probe-",".log");
        String java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
        String classpath=System.getProperty("surefire.test.class.path",System.getProperty("java.class.path"));
        var builder=new ProcessBuilder(java,"-Duser.timezone="+zone,"-cp",classpath,Probe.class.getName(),operation,Boolean.toString(utc),id)
                .redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("TEST_STORAGE_JDBC_URL",MYSQL.getJdbcUrl());
        builder.environment().put("TEST_STORAGE_JDBC_USER",MYSQL.getUsername());
        builder.environment().put("TEST_STORAGE_JDBC_PASSWORD",MYSQL.getPassword());
        Process process=builder.start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();throw new IllegalStateException("Timezone probe timed out");}
        assertThat(process.exitValue()).as("timezone probe exit").isZero();
        return Files.readAllLines(log).stream().filter(line->line.startsWith("TZ_PROBE_RESULT="))
                .reduce((a,b)->b).orElseThrow().substring("TZ_PROBE_RESULT=".length());
    }
    public static class Probe {
        public static void main(String[] args) {
            var builder=new org.hibernate.boot.registry.StandardServiceRegistryBuilder().applySettings(Map.of(
                "hibernate.connection.driver_class","com.mysql.cj.jdbc.Driver","hibernate.connection.url",System.getenv("TEST_STORAGE_JDBC_URL"),
                "hibernate.connection.username",System.getenv("TEST_STORAGE_JDBC_USER"),"hibernate.connection.password",System.getenv("TEST_STORAGE_JDBC_PASSWORD"),
                "hibernate.dialect","org.hibernate.dialect.MySQLDialect","hibernate.hbm2ddl.auto","none"));
            if(Boolean.parseBoolean(args[1])) builder.applySetting("hibernate.jdbc.time_zone","UTC");
            var registry=builder.build();
            try(var factory=new org.hibernate.boot.MetadataSources(registry).addAnnotatedClass(MediaAssetEntity.class).buildMetadata().buildSessionFactory();var session=factory.openSession()) {
                if(args[0].equals("write")) {
                    var tx=session.beginTransaction();var e=new MediaAssetEntity();var instant=Instant.parse("2026-11-13T12:00:00Z");
                    e.setOrigin(MediaOrigin.UPLOAD);e.setSourceContext(MediaContext.IDENTITY_PROFILE);e.setUploadedByUserId(990346L);e.setOwnerUserId(990346L);
                    e.setSubjectType(MediaSubjectType.USER);e.setSubjectId(990346L);e.setPurpose("PHOTO");e.setClassification(MediaClassification.CLASSIFIED);
                    e.setPhysicalState(MediaPhysicalState.PRESENT);e.setCreatedAt(instant);e.setUpdatedAt(instant);session.persist(e);tx.commit();
                    System.out.println("TZ_PROBE_RESULT="+e.getMediaId());
                } else if(args[0].equals("query")) {
                    var count=session.createQuery("select count(a) from MediaAssetEntity a where a.mediaId = :id and a.createdAt >= :fromDate and a.createdAt < :beforeDate",Long.class)
                        .setParameter("id",Long.parseLong(args[2]))
                        .setParameter("fromDate",Instant.parse("2026-11-13T11:59:00Z"))
                        .setParameter("beforeDate",Instant.parse("2026-11-13T12:01:00Z"))
                        .getSingleResult();
                    System.out.println("TZ_PROBE_RESULT="+count);
                } else {
                    var entity=session.find(MediaAssetEntity.class,Long.parseLong(args[2]));
                    System.out.println("TZ_PROBE_RESULT="+entity.getCreatedAt());
                }
            } finally {org.hibernate.boot.registry.StandardServiceRegistryBuilder.destroy(registry);}
        }
    }
}
