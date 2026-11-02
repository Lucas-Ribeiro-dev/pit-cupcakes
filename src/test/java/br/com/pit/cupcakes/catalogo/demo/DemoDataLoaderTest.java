package br.com.pit.cupcakes.catalogo.demo;

import br.com.pit.cupcakes.catalogo.entity.Product;
import br.com.pit.cupcakes.catalogo.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataLoaderTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private DemoDataLoader demoDataLoader;

    @Test
    @DisplayName("TC08 - Quando o banco estiver vazio, deve inserir os 4 cupcakes de demonstração")
    void shouldSeedDemoProductsWhenRepositoryIsEmpty() {
        when(productRepository.count()).thenReturn(0L);

        demoDataLoader.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Product>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(captor.capture());

        List<Product> savedProducts = captor.getValue();
        assertThat(savedProducts).hasSize(4);

        long availableCount = savedProducts.stream().filter(Product::getAvailable).count();
        long unavailableCount = savedProducts.stream().filter(p -> !p.getAvailable()).count();
        long withoutImageCount = savedProducts.stream().filter(p -> p.getImagePath() == null).count();

        assertThat(availableCount).isEqualTo(3);
        assertThat(unavailableCount).isEqualTo(1);
        assertThat(withoutImageCount).isGreaterThanOrEqualTo(1);

        savedProducts.forEach(product -> {
            assertThat(product.getPrice()).isGreaterThan(BigDecimal.ZERO);
            assertThat(product.getName()).contains("Demonstração");
            assertThat(product.getDescription()).isNotBlank();
        });
    }

    @Test
    @DisplayName("TC08 - Quando o banco já contiver produtos, não deve duplicar registros")
    void shouldNotSeedWhenRepositoryAlreadyHasProducts() {
        when(productRepository.count()).thenReturn(4L);

        demoDataLoader.run();

        verify(productRepository, never()).saveAll(anyList());
    }
}

