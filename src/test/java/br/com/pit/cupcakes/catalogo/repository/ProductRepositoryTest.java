package br.com.pit.cupcakes.catalogo.repository;

import br.com.pit.cupcakes.catalogo.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Deve persistir e buscar produto por id")
    void shouldPersistAndFindById() {
        Product product = new Product(
                "Cupcake de Limão",
                "Massa de limão siciliano",
                new BigDecimal("11.50"),
                "/assets/img/cupcake-limao.svg",
                true
        );

        Product saved = productRepository.save(product);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0L);

        Optional<Product> found = productRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Cupcake de Limão");
        assertThat(found.get().getPrice()).isEqualByComparingTo("11.50");
    }

    @Test
    @DisplayName("Deve listar produtos ordenados por id crescente")
    void shouldFindAllOrderedByIdAsc() {
        Product p1 = new Product("Cupcake B", "Desc", new BigDecimal("10.00"), null, true);
        Product p2 = new Product("Cupcake A", "Desc", new BigDecimal("12.00"), null, true);

        productRepository.save(p1);
        productRepository.save(p2);

        List<Product> products = productRepository.findAllByOrderByIdAsc();
        assertThat(products).hasSizeGreaterThanOrEqualTo(2);

        for (int i = 0; i < products.size() - 1; i++) {
            assertThat(products.get(i).getId()).isLessThan(products.get(i + 1).getId());
        }
    }
}

