package br.com.pit.cupcakes.catalogo;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PostgresTestDatabaseGuardTest {

    private MockEnvironment validEnvironment() {
        return new MockEnvironment()
                .withProperty("spring.datasource.url", PostgresTestDatabaseGuard.TEST_DATABASE_URL)
                .withProperty("spring.datasource.driver-class-name", "org.postgresql.Driver")
                .withProperty("spring.datasource.username", "test-user")
                .withProperty("spring.datasource.password", "test-only")
                .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("spring.flyway.enabled", "true")
                .withProperty("spring.flyway.clean-disabled", "true");
    }

    @Test
    void shouldAcceptOnlyAuthorizedConfiguration() {
        PostgresTestDatabaseGuard.validateConfiguration(validEnvironment());
        for (String url : new String[]{"jdbc:postgresql://localhost:5432/cupcakes",
                "jdbc:postgresql://remote:5432/cupcakes_test", "jdbc:h2:mem:cupcakes_test"}) {
            assertThatThrownBy(() -> PostgresTestDatabaseGuard.validateConfiguration(
                    validEnvironment().withProperty("spring.datasource.url", url)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void shouldRejectMissingCredentialsAndUnsafeOverrides() {
        for (String[] override : new String[][]{{"spring.datasource.password", ""},
                {"spring.datasource.username", ""}, {"spring.jpa.hibernate.ddl-auto", "create-drop"},
                {"spring.flyway.enabled", "false"}, {"spring.flyway.clean-disabled", "false"},
                {"spring.flyway.url", PostgresTestDatabaseGuard.TEST_DATABASE_URL}}) {
            assertThatThrownBy(() -> PostgresTestDatabaseGuard.validateConfiguration(
                    validEnvironment().withProperty(override[0], override[1])))
                    .isInstanceOf(IllegalStateException.class);
        }
        MockEnvironment environment = validEnvironment();
        environment.setActiveProfiles("test-pg", "demo");
        assertThatThrownBy(() -> PostgresTestDatabaseGuard.validateConfiguration(environment))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectEffectiveDevelopmentDatabaseBeforeAnyCleanup() throws Exception {
        Connection connection = createConnection("cupcakes");
        assertThatThrownBy(() -> PostgresTestDatabaseGuard.verifyConnection(connection))
                .isInstanceOf(SQLException.class).hasMessageContaining("Banco efetivo não autorizado");
        verify(connection).close();
    }

    @Test
    void shouldAcceptEffectiveTestDatabase() throws Exception {
        Connection connection = createConnection("cupcakes_test");
        assertThat(PostgresTestDatabaseGuard.verifyConnection(connection)).isSameAs(connection);
        verify(connection, never()).close();
    }

    private Connection createConnection(String database) throws Exception {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        Statement statement = mock(Statement.class);
        ResultSet result = mock(ResultSet.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getURL()).thenReturn(PostgresTestDatabaseGuard.TEST_DATABASE_URL);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("select current_database()")).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getString(1)).thenReturn(database);
        return connection;
    }
}
