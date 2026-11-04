package com.jeepclub.backend.authorization;

import com.jeepclub.backend.iam.authorization.core.application.exception.userrole.UserRoleAlreadyExistsException;
import com.jeepclub.backend.iam.authorization.core.application.exception.rolepermission.RolePermissionAlreadyExistsException;
import com.jeepclub.backend.shared.authorization.PermissionCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.mysql.MySQLContainer;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class AuthorizationAdministrationMySqlIT extends AuthorizationAdministrationIntegrationTest {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.46")
            .withUsername("root").withPassword("synthetic-authorization-test-only");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username",MYSQL::getUsername);
        registry.add("spring.datasource.password",MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name",() -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform",() -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto",() -> "validate");
        registry.add("spring.flyway.enabled",() -> true);
    }
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate jdbc;
    enum Binding { USER_ROLE, ROLE_PERMISSION }

    @ParameterizedTest @EnumSource(Binding.class)
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    void concurrentAssignmentsWaitOnTheRealParentLockAndLeaveOneBinding(Binding type) throws Exception {
        Long role=actorRole.getId();
        Long permission=permission(PermissionCode.AUTHORIZATION_ROLE_READ);
        Runnable assign=type==Binding.USER_ROLE
                ? () -> users.assignRoleToUser(target,role) : () -> rolePermissions.assignPermissionToRole(role,permission);
        var locked=new CountDownLatch(1);
        var contenderStarted=new CountDownLatch(1);
        var executor=Executors.newFixedThreadPool(2);
        try {
            var holder=executor.submit(() -> transactions.executeWithoutResult(status -> {
                if(type==Binding.USER_ROLE) identityLock.byId(target).orElseThrow();
                else roleRepository.findByIdForUpdate(role).orElseThrow();
                Long connection=jdbc.queryForObject("SELECT CONNECTION_ID()",Long.class);
                locked.countDown();
                await(contenderStarted);
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
                boolean waited=false;
                while(System.nanoTime()<deadline) {
                    Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits w "
                            +"JOIN performance_schema.threads b ON b.thread_id=w.blocking_thread_id WHERE b.processlist_id=?",
                            Integer.class,connection);
                    if(count!=null && count>0) { waited=true; break; }
                    Thread.onSpinWait();
                }
                assertThat(waited).as("contender reached a real MySQL lock wait").isTrue();
                assign.run();
            }));
            await(locked);
            var contender=executor.submit(() -> {
                contenderStarted.countDown();
                assertThatThrownBy(assign::run).isInstanceOf(type==Binding.USER_ROLE
                        ? UserRoleAlreadyExistsException.class : RolePermissionAlreadyExistsException.class);
            });
            holder.get(30,TimeUnit.SECONDS);
            contender.get(30,TimeUnit.SECONDS);
            if(type==Binding.USER_ROLE) assertThat(bindings.findRolesByUserId(target))
                    .extracting(com.jeepclub.backend.iam.authorization.core.domain.model.Role::getId).containsExactly(role);
            else assertThat(permissionBindings.findPermissionsByRoleId(role))
                    .extracting(com.jeepclub.backend.iam.authorization.core.domain.model.Permission::getId).containsExactly(permission);
        } finally { locked.countDown(); contenderStarted.countDown(); executor.shutdownNow(); }
    }
    private static void await(CountDownLatch latch) {
        try { assertThat(latch.await(20,TimeUnit.SECONDS)).isTrue(); }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }
}
