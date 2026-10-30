package com.jeepclub.backend.memberships;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import javax.sql.DataSource;
import java.lang.reflect.*;
import java.sql.*;

/** Fails the real JDBC commit before it reaches the database, after the service and flush succeeded. */
@TestConfiguration
class CommitFailureInjection {
    static final ThreadLocal<Boolean> ARMED = ThreadLocal.withInitial(() -> false);
    @Bean static BeanPostProcessor commitFailureDataSource() {
        return new BeanPostProcessor() {
            @Override public Object postProcessAfterInitialization(Object bean, String name) {
                if (!(bean instanceof DataSource source)) return bean;
                return new DelegatingDataSource(source) {
                    @Override public Connection getConnection() throws SQLException { return wrap(super.getConnection()); }
                    @Override public Connection getConnection(String u, String p) throws SQLException { return wrap(super.getConnection(u,p)); }
                    private Connection wrap(Connection connection) {
                        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                            if (method.getName().equals("commit") && ARMED.get()) {
                                ARMED.remove(); connection.rollback(); throw new SQLException("Synthetic JDBC commit failure");
                            }
                            try { return method.invoke(connection, args); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                        });
                    }
                };
            }
        };
    }
}
