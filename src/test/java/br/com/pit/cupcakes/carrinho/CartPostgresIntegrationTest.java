package br.com.pit.cupcakes.carrinho;

import br.com.pit.cupcakes.catalogo.PostgresTestDatabaseGuard;
import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import br.com.pit.cupcakes.pedido.StoreFlowBrowserServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test-pg")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("postgres")
@ContextConfiguration(initializers = PostgresTestDatabaseGuard.class)
class CartPostgresIntegrationTest {
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductRepository products;
    @Autowired MockMvc mvc;

    @Test void readsCurrentPriceAndAvailabilityFromProtectedPostgres() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("select current_database()")) {
                org.assertj.core.api.Assertions.assertThat(result.next()).isTrue();
                org.assertj.core.api.Assertions.assertThat(result.getString(1)).isEqualTo("cupcakes_test");
            }
        }
        StoreFlowBrowserServer.cleanupFlowFixtures(jdbc, "admin.fluxo.isolado@example.com");
        Product product = products.save(new Product("Carrinho " + UUID.randomUUID(), "Fixture isolada",
                new BigDecimal("10.00"), null, true));
        long id = product.getId();
        MockHttpSession session = new MockHttpSession();
        try {
            mvc.perform(post("/api/carrinho/itens").session(session).with(csrf()).contentType("application/json")
                    .content("{\"produtoId\":" + id + ",\"quantidade\":2}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value("20.00"));
            product.setPrice(new BigDecimal("11.25"));
            product = products.saveAndFlush(product);
            mvc.perform(get("/api/carrinho").session(session)).andExpect(jsonPath("$.subtotal").value("22.50"));
            product.setAvailable(false);
            product = products.saveAndFlush(product);
            mvc.perform(get("/api/carrinho").session(session))
                    .andExpect(jsonPath("$.itens[0].motivoBloqueio").value("PRODUTO_INDISPONIVEL"))
                    .andExpect(jsonPath("$.subtotal").value("0.00"));
            products.deleteById(id);
            mvc.perform(get("/api/carrinho").session(session))
                    .andExpect(jsonPath("$.itens[0].motivoBloqueio").value("PRODUTO_NAO_ENCONTRADO"));
        } finally {
            if (products.existsById(id)) products.deleteById(id);
        }
    }
}
