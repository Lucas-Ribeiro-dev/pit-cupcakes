package br.com.pit.cupcakes.catalogo;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

public class PostgresTestDatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String TEST_DATABASE_URL = "jdbc:postgresql://localhost:5432/cupcakes_test";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        validateConfiguration(context.getEnvironment());
        context.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource source) {
                    return new DelegatingDataSource(source) {
                        @Override
                        public Connection getConnection() throws SQLException {
                            return verifyConnection(super.getConnection());
                        }

                        @Override
                        public Connection getConnection(String username, String password) throws SQLException {
                            return verifyConnection(super.getConnection(username, password));
                        }
                    };
                }
                return bean;
            }
        });
    }

    static void validateConfiguration(Environment environment) {
        if (!TEST_DATABASE_URL.equals(environment.getProperty("spring.datasource.url"))
                || !"org.postgresql.Driver".equals(environment.getProperty("spring.datasource.driver-class-name"))
                || !"validate".equals(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                || !environment.getProperty("spring.flyway.enabled", Boolean.class, false)
                || !environment.getProperty("spring.flyway.clean-disabled", Boolean.class, false)
                || environment.getProperty("spring.flyway.url") != null
                || Arrays.asList(environment.getActiveProfiles()).contains("demo")) {
            throw new IllegalStateException("Integração bloqueada: use somente PostgreSQL localhost:5432/cupcakes_test, Flyway habilitado sem conexão alternativa e ddl-auto validate.");
        }
        if (environment.getProperty("spring.datasource.username", "").isBlank()
                || environment.getProperty("spring.datasource.password", "").isBlank()) {
            throw new IllegalStateException("Integração sem credenciais: configure DB_TEST_USERNAME e DB_TEST_PASSWORD localmente; não use credenciais de desenvolvimento como alternativa.");
        }
    }

    static Connection verifyConnection(Connection connection) throws SQLException {
        try {
            if (!TEST_DATABASE_URL.equals(connection.getMetaData().getURL())
                    || !"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                throw new SQLException("Destino da integração não autorizado.");
            }
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("select current_database()")) {
                if (!result.next() || !"cupcakes_test".equals(result.getString(1))) {
                    throw new SQLException("Banco efetivo não autorizado: a integração exige cupcakes_test.");
                }
            }
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }
}
