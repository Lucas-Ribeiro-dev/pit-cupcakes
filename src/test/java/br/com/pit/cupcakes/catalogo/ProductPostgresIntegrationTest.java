package br.com.pit.cupcakes.catalogo;

import br.com.pit.cupcakes.catalogo.dto.ProductResponse;
import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import br.com.pit.cupcakes.compartilhado.exception.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-pg")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("postgres")
@ContextConfiguration(initializers = PostgresTestDatabaseGuard.class)
public class ProductPostgresIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void verifyDatabaseBeforeCleanup() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            PostgresTestDatabaseGuard.verifyConnection(connection);
        }
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("PG-TC01 - Consultar catálogo populado no PostgreSQL real")
    void shouldReturnPopulatedCatalogFromPostgres() {
        productRepository.deleteAll();

        Product p1 = new Product(
                "Cupcake Teste 1",
                "Descrição do teste 1",
                new BigDecimal("10.00"),
                "/assets/img/teste1.svg",
                true
        );
        Product p2 = new Product(
                "Cupcake Teste 2",
                "Descrição do teste 2",
                new BigDecimal("15.00"),
                null,
                false
        );
        productRepository.saveAll(List.of(p1, p2));

        ResponseEntity<List<ProductResponse>> response = restTemplate.exchange(
                "/api/produtos",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);

        ProductResponse first = response.getBody().get(0);
        assertThat(first.name()).isEqualTo("Cupcake Teste 1");
        assertThat(first.price()).isEqualTo("10.00");
        assertThat(first.available()).isTrue();
    }

    @Test
    @DisplayName("PG-TC02 - Consultar catálogo vazio no banco isolado de teste")
    void shouldReturnEmptyListWhenDatabaseIsEmpty() {
        productRepository.deleteAll();

        ResponseEntity<List<ProductResponse>> response = restTemplate.exchange(
                "/api/produtos",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("PG-TC03 - Consultar ID existente com dados persistidos no PostgreSQL")
    void shouldReturnProductByIdWhenExists() {
        productRepository.deleteAll();

        Product product = new Product(
                "Cupcake Especial",
                "Massa fofa",
                new BigDecimal("12.50"),
                "/assets/img/especial.svg",
                true
        );
        Product saved = productRepository.save(product);

        ResponseEntity<ProductResponse> response = restTemplate.getForEntity(
                "/api/produtos/" + saved.getId(),
                ProductResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(saved.getId());
        assertThat(response.getBody().price()).isEqualTo("12.50");
    }

    @Test
    @DisplayName("PG-TC04 - Consultar ID inexistente retorna 404 e código PRODUTO_NAO_ENCONTRADO")
    void shouldReturn404WhenIdDoesNotExist() {
        productRepository.deleteAll();

        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                "/api/produtos/999999",
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("PRODUTO_NAO_ENCONTRADO");
    }

    @Test
    @DisplayName("PG-TC05 - Consultar ID zero ou negativo na API retorna 400 e ID_INVALIDO")
    void shouldReturn400WhenIdIsInvalid() {
        ResponseEntity<ErrorResponse> responseZero = restTemplate.getForEntity(
                "/api/produtos/0",
                ErrorResponse.class
        );
        assertThat(responseZero.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseZero.getBody()).isNotNull();
        assertThat(responseZero.getBody().code()).isEqualTo("ID_INVALIDO");

        ResponseEntity<ErrorResponse> responseAlpha = restTemplate.getForEntity(
                "/api/produtos/invalido",
                ErrorResponse.class
        );
        assertThat(responseAlpha.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseAlpha.getBody()).isNotNull();
        assertThat(responseAlpha.getBody().code()).isEqualTo("ID_INVALIDO");
    }
}
